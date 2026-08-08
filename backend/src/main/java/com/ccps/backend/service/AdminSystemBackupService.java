package com.ccps.backend.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminSystemBackupResponse.Item;
import com.ccps.backend.dto.AdminSystemBackupResponse.RestoreResult;

@Service
public class AdminSystemBackupService {
    private static final Pattern JDBC_MYSQL = Pattern.compile("jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?]+).*");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final long MAX_RESTORE_UNCOMPRESSED_BYTES = 20L * 1024 * 1024 * 1024;
    private static final String CONFIRMATION = "RESTORE CCPS";

    private final Path backupRoot;
    private final Path uploadsRoot;
    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String dumpCommand;
    private final String mysqlCommand;

    public AdminSystemBackupService(
            @Value("${ccps.backup.root:backups}") String backupRoot,
            @Value("${ccps.storage.document-root:uploads}") String uploadsRoot,
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${ccps.backup.mysqldump-command:mysqldump}") String dumpCommand,
            @Value("${ccps.backup.mysql-command:mysql}") String mysqlCommand) {
        this.backupRoot = Path.of(backupRoot).toAbsolutePath().normalize();
        this.uploadsRoot = Path.of(uploadsRoot).toAbsolutePath().normalize();
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.dumpCommand = dumpCommand;
        this.mysqlCommand = mysqlCommand;
    }

    @Transactional(readOnly = true)
    public List<Item> listBackups() {
        try {
            Files.createDirectories(backupRoot);
            try (var files = Files.list(backupRoot)) {
                return files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip"))
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .limit(50)
                        .map(this::toItem)
                        .toList();
            }
        } catch (IOException exception) {
            throw serverError("Unable to list system backups", exception);
        }
    }

    public BackupArtifact createManualBackup() {
        return createBackup("manual");
    }

    public BackupArtifact requireBackup(String fileName) {
        String safeName = Path.of(fileName == null ? "" : fileName).getFileName().toString();
        Path file = backupRoot.resolve(safeName).normalize();
        if (!file.startsWith(backupRoot) || !Files.isRegularFile(file) || !safeName.endsWith(".zip")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup file not found");
        }
        try { return new BackupArtifact(file, safeName, Files.size(file)); }
        catch (IOException exception) { throw serverError("Unable to read backup file", exception); }
    }

    public RestoreResult restore(MultipartFile archive, String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restore confirmation phrase is incorrect");
        }
        if (archive == null || archive.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a CCPS backup ZIP file");
        }

        BackupArtifact safety = createBackup("pre-restore");
        Path workspace = null;
        try {
            Files.createDirectories(backupRoot);
            workspace = Files.createTempDirectory(backupRoot, "restore-");
            Path stagedArchive = workspace.resolve("restore.zip");
            archive.transferTo(stagedArchive);
            Path extracted = workspace.resolve("extracted");
            extractBackup(stagedArchive, extracted);
            Path sql = extracted.resolve("database.sql");
            Path stagedUploads = extracted.resolve("uploads");
            if (!Files.isRegularFile(sql) || !Files.isDirectory(stagedUploads)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid backup: database.sql or uploads directory is missing");
            }

            restoreDatabase(sql, workspace.resolve("mysql-restore.log"));
            replaceUploads(stagedUploads, workspace.resolve("previous-uploads"));
            return new RestoreResult("System restore completed. Restart the backend before continuing.", safety.fileName());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Restore failed. Safety backup retained as " + safety.fileName(), exception);
        } finally {
            deleteTreeQuietly(workspace);
        }
    }

    private BackupArtifact createBackup(String type) {
        Path workspace = null;
        try {
            Files.createDirectories(backupRoot);
            workspace = Files.createTempDirectory(backupRoot, "build-");
            Path dump = workspace.resolve("database.sql");
            runDatabaseDump(dump, workspace.resolve("mysqldump.log"));
            String fileName = "ccps-" + type + "-" + LocalDateTime.now().format(STAMP) + ".zip";
            Path target = backupRoot.resolve(fileName).normalize();
            if (!target.startsWith(backupRoot)) throw new IOException("Invalid backup target");
            writeBackupZip(target, dump, type);
            return new BackupArtifact(target, fileName, Files.size(target));
        } catch (Exception exception) {
            throw serverError("Unable to create complete system backup", exception);
        } finally {
            deleteTreeQuietly(workspace);
        }
    }

    private void runDatabaseDump(Path dump, Path log) throws Exception {
        DatabaseTarget target = databaseTarget();
        List<String> command = new ArrayList<>();
        command.add(dumpCommand);
        command.add("--host=" + target.host());
        command.add("--port=" + target.port());
        command.add("--user=" + databaseUsername);
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add("--default-character-set=utf8mb4");
        command.add("--add-drop-table");
        command.add("--databases");
        command.add(target.database());
        command.add("--result-file=" + dump.toAbsolutePath());
        runProcess(command, null, log, "mysqldump");
        if (!Files.isRegularFile(dump) || Files.size(dump) == 0) throw new IOException("Database dump is empty");
    }

    private void restoreDatabase(Path sql, Path log) throws Exception {
        DatabaseTarget target = databaseTarget();
        List<String> command = List.of(mysqlCommand, "--host=" + target.host(), "--port=" + target.port(),
                "--user=" + databaseUsername, "--default-character-set=utf8mb4");
        runProcess(command, sql, log, "mysql restore");
    }

    private void runProcess(List<String> command, Path stdin, Path log, String label) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("MYSQL_PWD", databasePassword == null ? "" : databasePassword);
        builder.redirectErrorStream(true);
        builder.redirectOutput(log.toFile());
        Process process = builder.start();
        if (stdin != null) {
            try (OutputStream output = new BufferedOutputStream(process.getOutputStream());
                    InputStream input = new BufferedInputStream(Files.newInputStream(stdin))) {
                input.transferTo(output);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String details = Files.isRegularFile(log) ? Files.readString(log, StandardCharsets.UTF_8) : "";
            if (details.length() > 2000) details = details.substring(details.length() - 2000);
            throw new IOException(label + " exited with code " + exitCode + ": " + details);
        }
    }

    private void writeBackupZip(Path target, Path dump, String type) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            putText(zip, "manifest.json", "{\"format\":\"ccps-full-backup-v1\",\"type\":\"" + type
                    + "\",\"createdAt\":\"" + LocalDateTime.now() + "\"}");
            putFile(zip, dump, "database.sql");
            zip.putNextEntry(new ZipEntry("uploads/"));
            zip.closeEntry();
            if (Files.isDirectory(uploadsRoot)) {
                try (var paths = Files.walk(uploadsRoot)) {
                    for (Path path : paths.sorted().toList()) {
                        if (path.equals(uploadsRoot) || !Files.isRegularFile(path)) continue;
                        String relative = uploadsRoot.relativize(path).toString().replace('\\', '/');
                        putFile(zip, path, "uploads/" + relative);
                    }
                }
            }
        }
    }

    private void extractBackup(Path archive, Path destination) throws IOException {
        Files.createDirectories(destination);
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) throw new IOException("Unsafe ZIP entry: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            total += read;
                            if (total > MAX_RESTORE_UNCOMPRESSED_BYTES) throw new IOException("Backup exceeds restore size limit");
                            output.write(buffer, 0, read);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private void replaceUploads(Path stagedUploads, Path rollback) throws IOException {
        Files.createDirectories(uploadsRoot.getParent());
        boolean movedCurrent = false;
        try {
            if (Files.exists(uploadsRoot)) {
                move(uploadsRoot, rollback);
                movedCurrent = true;
            }
            move(stagedUploads, uploadsRoot);
            if (movedCurrent) deleteTreeQuietly(rollback);
        } catch (IOException exception) {
            if (!Files.exists(uploadsRoot) && movedCurrent && Files.exists(rollback)) move(rollback, uploadsRoot);
            throw exception;
        }
    }

    private void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(source, target); }
    }

    private DatabaseTarget databaseTarget() {
        Matcher matcher = JDBC_MYSQL.matcher(URLDecoder.decode(databaseUrl, StandardCharsets.UTF_8));
        if (!matcher.matches()) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported database URL");
        return new DatabaseTarget(matcher.group(1), matcher.group(2) == null ? "3306" : matcher.group(2), matcher.group(3));
    }

    private void putText(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void putFile(ZipOutputStream zip, Path source, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = new BufferedInputStream(Files.newInputStream(source))) { input.transferTo(zip); }
        zip.closeEntry();
    }

    private Item toItem(Path path) {
        try {
            String name = path.getFileName().toString();
            String type = name.contains("pre-restore") ? "pre_restore" : "manual";
            return new Item(name, Files.size(path), LocalDateTime.ofInstant(lastModified(path).toInstant(), ZoneId.systemDefault()), type);
        } catch (IOException exception) { throw serverError("Unable to read backup metadata", exception); }
    }

    private FileTime lastModified(Path path) {
        try { return Files.getLastModifiedTime(path); }
        catch (IOException ignored) { return FileTime.fromMillis(0); }
    }

    private void deleteTreeQuietly(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
        } catch (IOException ignored) { }
    }

    private ResponseStatusException serverError(String message, Exception exception) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, exception);
    }

    private record DatabaseTarget(String host, String port, String database) { }
    public record BackupArtifact(Path path, String fileName, long size) { }
}
