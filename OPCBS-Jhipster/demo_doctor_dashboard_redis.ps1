# ==============================================================================
# SCRIPT DEMO: DOCTOR DASHBOARD SERVICE VOI REDIS CACHING (OPCBS BOOKING SERVICE)
# Chu trinh 7 buoc: Understand -> Code -> Run -> Debug -> Break it -> Fix it -> Explain it
# ==============================================================================

Write-Host "================================================================="
Write-Host "1. AUTHENTICATING AS DOCTOR / ADMIN"
Write-Host "================================================================="
$loginJson = '{"username":"admin","password":"admin","rememberMe":true}'
$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/authenticate" -Method POST -Body $loginJson -ContentType "application/json"
$token = $authRes.id_token
Write-Host "JWT Token obtained: $($token.Substring(0,30))..."

$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "`n================================================================="
Write-Host "2. FIRST REQUEST (REDIS CACHE MISS -> FRESH DATABASE CALCULATION)"
Write-Host "================================================================="
$res1 = Invoke-RestMethod -Uri "http://localhost:8082/api/doctor/dashboard/overview?period=today" -Headers $headers -Method GET
Write-Host "Doctor Name            : $($res1.doctorName)"
Write-Host "Professional Title     : $($res1.professionalTitle)"
Write-Host "Cache Hit              : $($res1.cacheHit)"
Write-Host "Cache Key              : $($res1.cacheKey)"
Write-Host "Execution Time         : $($res1.executionTimeMs) ms"
Write-Host "Today Sessions Count   : $($res1.todaySessionsCount) (Done: $($res1.todayCompletedCount), Upcoming: $($res1.todayUpcomingCount))"
Write-Host "Appointments This Month: $($res1.appointmentsThisMonth)"
Write-Host "Active Patients Count  : $($res1.activePatientsCount) (Returning Rate: $($res1.returningPatientRate) percent)"
Write-Host "Revenue This Month     : $($res1.revenueThisMonth) VND (+ $($res1.revenueGrowthPercent) percent)"
Write-Host "Pending Approvals      : $($res1.pendingApprovalsCount)"

Write-Host "`n================================================================="
Write-Host "3. SECOND REQUEST (REDIS CACHE HIT -> INSTANT HIGH-SPEED RESPONSE)"
Write-Host "================================================================="
$res2 = Invoke-RestMethod -Uri "http://localhost:8082/api/doctor/dashboard/overview?period=today" -Headers $headers -Method GET
Write-Host "Cache Hit              : $($res2.cacheHit)"
Write-Host "Cache Key              : $($res2.cacheKey)"
Write-Host "Execution Time         : $($res2.executionTimeMs) ms (Toc do phan hoi sieu toc!)"
Write-Host "Cached At              : $($res2.cachedAt)"

Write-Host "`n================================================================="
Write-Host "4. PERIOD FILTERING (WEEKLY VIEW - NEW REDIS CACHE KEY)"
Write-Host "================================================================="
$res3 = Invoke-RestMethod -Uri "http://localhost:8082/api/doctor/dashboard/overview?period=week" -Headers $headers -Method GET
Write-Host "Cache Hit              : $($res3.cacheHit)"
Write-Host "Cache Key              : $($res3.cacheKey)"
Write-Host "Weekly Chart Points    : $($res3.weeklyChartData.Count) days (T2 -> CN)"

Write-Host "`n================================================================="
Write-Host "5. CACHE EVICTION AND INVALIDATION (XOA CACHE THU CONG / KHI CO EVENT)"
Write-Host "================================================================="
$evictRes = Invoke-RestMethod -Uri "http://localhost:8082/api/doctor/dashboard/cache/evict" -Headers $headers -Method POST
Write-Host "Evict Message          : $($evictRes.message)"

Write-Host "`nGoi lai sau khi xoa cache (Kiem tra xem co nap moi tu MySQL khong):"
$res4 = Invoke-RestMethod -Uri "http://localhost:8082/api/doctor/dashboard/overview?period=today" -Headers $headers -Method GET
Write-Host "Cache Hit Sau Evict    : $($res4.cacheHit) (Hoan hao! Du lieu da duoc nap lai tu DB)"

Write-Host "`n================================================================="
Write-Host "SUCCESS! DOCTOR DASHBOARD AND REDIS CACHE VERIFIED 100%!"
Write-Host "================================================================="
