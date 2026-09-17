function decoder(charset) {
  try { return new TextDecoder(charset || 'utf-8'); }
  catch { return new TextDecoder('utf-8'); }
}

function decodeQuotedPrintable(value, charset) {
  const bytes = [];
  for (let index = 0; index < value.length; index += 1) {
    if (value[index] === '=' && /^[0-9a-f]{2}$/i.test(value.slice(index + 1, index + 3))) {
      bytes.push(Number.parseInt(value.slice(index + 1, index + 3), 16));
      index += 2;
    } else {
      bytes.push(value[index] === '_' ? 32 : value.charCodeAt(index));
    }
  }
  return decoder(charset).decode(new Uint8Array(bytes));
}

function decodeBase64(value, charset) {
  const binary = atob(value);
  return decoder(charset).decode(Uint8Array.from(binary, character => character.charCodeAt(0)));
}

export function decodeMimeFilename(value) {
  const source = String(value || '').replace(/\?=\s+(?==\?)/g, '?=');
  try {
    const decoded = source.replace(/=\?([^?]+)\?([bq])\?([^?]*)\?=/gi, (_, charset, encoding, body) => (
      encoding.toLowerCase() === 'b'
        ? decodeBase64(body, charset)
        : decodeQuotedPrintable(body, charset)
    ));
    if (decoded !== source) return decoded;

    // 兼容部分旧资料中缺少结尾“?=”的 MIME 文件名。
    const malformed = source.match(/^=\?([^?]+)\?([bq])\?(.+?)(?:\?=)?$/i);
    if (!malformed) return source;
    const [, charset, encoding, body] = malformed;
    return encoding.toLowerCase() === 'b'
      ? decodeBase64(body, charset)
      : decodeQuotedPrintable(body, charset);
  } catch {
    return source;
  }
}
