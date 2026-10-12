# Script test upload file trực tiếp không qua Postman
$testFile = "test_license_doctor.pdf"
[System.IO.File]::WriteAllBytes($testFile, [System.Text.Encoding]::UTF8.GetBytes("%PDF-1.4 Fake Doctor Medical Certificate Content for Debugging"))

$boundary = [System.Guid]::NewGuid().ToString()
$fileBytes = [System.IO.File]::ReadAllBytes($testFile)
$fileHeader = "--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"test_license_doctor.pdf`"`r`nContent-Type: application/pdf`r`n`r`n"
$fileFooter = "`r`n--$boundary`r`nContent-Disposition: form-data; name=`"documentType`"`r`n`r`nMEDICAL_LICENSE`r`n--$boundary--`r`n"

$bodyStream = [System.IO.MemoryStream]::new()
$writer = [System.IO.BinaryWriter]::new($bodyStream)
$writer.Write([System.Text.Encoding]::UTF8.GetBytes($fileHeader))
$writer.Write($fileBytes)
$writer.Write([System.Text.Encoding]::UTF8.GetBytes($fileFooter))

Write-Host "Test file created. Ready to upload."
