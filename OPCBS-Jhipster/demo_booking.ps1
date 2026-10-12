$loginJson = '{"username":"admin","password":"admin","rememberMe":true}'
$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/authenticate" -Method POST -Body $loginJson -ContentType "application/json"
$token = $authRes.id_token
Write-Host ">>> 1. Authenticated as admin. Token: $($token.Substring(0,30))..."

$headers = @{ "Authorization" = "Bearer $token" }

# 2. Tạo một khung giờ mới (Future Slot) cho Bác sĩ ID = 1 vào ngày mai
$futureSlotPayload = @{
    startTime        = "2026-10-10T09:00:00Z"
    endTime          = "2026-10-10T10:00:00Z"
    consultationMode = "ONLINE"
    status           = "AVAILABLE"
    notes            = "Slot demo khoa kham tam ly"
    doctor           = @{ id = 1 }
} | ConvertTo-Json

Write-Host ">>> 2. Creating Future Availability Slot via POST /api/availability-slots..."
$createdSlot = Invoke-RestMethod -Uri "http://localhost:8082/api/availability-slots" -Headers $headers -Method POST -Body $futureSlotPayload -ContentType "application/json"
Write-Host ">>> Created Slot ID: $($createdSlot.id) | Doctor ID: $($createdSlot.doctor.id) | Status: $($createdSlot.status) | Time: $($createdSlot.startTime) -> $($createdSlot.endTime)"

# 3. Tiến hành Đặt Lịch (Book Appointment)
$bookPayload = @{
    slotId           = $createdSlot.id
    contactPhone     = "0912345678"
    patientAddress   = "123 Pham Van Dong, Cau Giay, Ha Noi"
    consultationMode = "ONLINE"
    reason           = "Khung hoang ap luc cong viec va mat ngu keo dai"
    symptoms         = "Dau dau nhe, kho ngu, tim dap nhanh khi lo au"
    medicalHistory   = "Chua tung dieu tri tam ly truoc day"
    expectations     = "Mong muon bac si tu van lieu trinh phuc hoi cam xuc"
} | ConvertTo-Json

Write-Host ">>> 3. Patient booking the slot via POST /api/patient/appointments/book ..."
$bookingRes = Invoke-RestMethod -Uri "http://localhost:8082/api/patient/appointments/book" -Headers $headers -Method POST -Body $bookPayload -ContentType "application/json"

Write-Host "================================================================="
Write-Host ">>> 4. BOOKING SUCCESSFUL!"
Write-Host "================================================================="
Write-Host "Appointment ID  : $($bookingRes.id)"
Write-Host "Booking Code    : $($bookingRes.bookingCode)"
Write-Host "Status          : $($bookingRes.status)"
Write-Host "Doctor Name     : $($bookingRes.doctor.fullName)"
Write-Host "Patient Name    : $($bookingRes.patient.fullName)"
Write-Host "Start Time      : $($bookingRes.scheduledStart)"
Write-Host "Consultation    : $($bookingRes.consultationMode)"
Write-Host "Reason          : $($bookingRes.reason)"

# 4. Kiểm tra lại trạng thái của Slot trên DB xem đã tự chuyển sang BOOKED chưa
$slotAfterBooking = Invoke-RestMethod -Uri "http://localhost:8082/api/availability-slots/$($createdSlot.id)" -Headers $headers -Method GET
Write-Host ">>> 5. Database Verification: Slot ID $($createdSlot.id) Status in DB is now: $($slotAfterBooking.status)"
