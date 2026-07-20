Option Explicit

Dim inputPath, outputPath, word, document
inputPath = WScript.Arguments(0)
outputPath = WScript.Arguments(1)

Set word = CreateObject("Word.Application")
word.Visible = False
word.DisplayAlerts = 0
Set document = word.Documents.Open(inputPath, False, True)
document.ExportAsFixedFormat outputPath, 17
document.Close False
word.Quit

Set document = Nothing
Set word = Nothing
