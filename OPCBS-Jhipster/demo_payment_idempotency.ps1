$loginJson = '{"username":"admin","password":"admin","rememberMe":true}'
$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/authenticate" -Method POST -Body $loginJson -ContentType "application/json"
$token = $authRes.id_token
Write-Host ">>> 1. Authenticated as admin. JWT: $($token.Substring(0,30))..."

$headers = @{ "Authorization" = "Bearer $token" }

# 2. Subscribe package
$subRes = Invoke-RestMethod -Uri "http://localhost:8087/api/doctor/subscriptions/subscribe?packageId=1" -Headers $headers -Method POST
$txnCode = $subRes.transactionCode
Write-Host ">>> 2. Subscription Created: ID $($subRes.subscriptionId) | Txn: $txnCode | Package: $($subRes.packageName)"

# 3. Create VNPay URL
$urlRes = Invoke-RestMethod -Uri "http://localhost:8087/api/payments/vnpay/create-url?transactionCode=$txnCode" -Headers $headers -Method POST
Write-Host ">>> 3. VNPay URL Generated: $($urlRes.paymentUrl.Substring(0, 60))..."

# Helper function
function Compute-HmacSHA512([string]$key, [string]$data) {
    $hmac = New-Object System.Security.Cryptography.HMACSHA512
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($key)
    $hashBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($data))
    return -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
}

$secret = "OPCBSVNPAYHASHSECRETDEMO202610"
$amount = "49900000"
$amp = [char]38

$pList = New-Object System.Collections.Generic.List[string]
$pList.Add("vnp_Amount=$amount")
$pList.Add("vnp_BankCode=NCB")
$pList.Add("vnp_CardType=ATM")
$pList.Add("vnp_OrderInfo=Thanh+toan+goi+dich+vu")
$pList.Add("vnp_PayDate=20261005120000")
$pList.Add("vnp_ResponseCode=00")
$pList.Add("vnp_TmnCode=OPCBSDEMO")
$pList.Add("vnp_TransactionNo=14592831")
$pList.Add("vnp_TransactionStatus=00")
$pList.Add("vnp_TxnRef=$txnCode")

$rawHash = [string]::Join($amp.ToString(), $pList)
$hash = Compute-HmacSHA512 $secret $rawHash

$callbackUri = "http://localhost:8087/api/payments/vnpay/callback?$rawHash$amp" + "vnp_SecureHash=$hash"

Write-Host ">>> 4. Executing FIRST Callback (Fresh Execution)..."
$call1 = Invoke-RestMethod -Uri $callbackUri -Method GET
Write-Host ">>> [CALL 1 Result] Status: $($call1.status) | isIdempotentReplay: $($call1.isIdempotentReplay) | Expiration: $($call1.expirationDate)"

Write-Host ">>> 5. Executing SECOND Callback (VNPay Retry / User Refresh - IDEMPOTENCY GUARD)..."
$call2 = Invoke-RestMethod -Uri $callbackUri -Method GET
Write-Host ">>> [CALL 2 Result] Status: $($call2.status) | isIdempotentReplay: $($call2.isIdempotentReplay) | Expiration: $($call2.expirationDate)"

Write-Host ">>> 6. Executing THIRD Callback (VNPay 3rd Retry)..."
$call3 = Invoke-RestMethod -Uri $callbackUri -Method GET
Write-Host ">>> [CALL 3 Result] Status: $($call3.status) | isIdempotentReplay: $($call3.isIdempotentReplay) | Expiration: $($call3.expirationDate)"

Write-Host "================================================================="
if ($call2.isIdempotentReplay -and ($call1.expirationDate -eq $call2.expirationDate)) {
    Write-Host ">>> VERIFICATION PASSED: Idempotency Protected Against Duplicate Webhooks!"
}
Write-Host "================================================================="
