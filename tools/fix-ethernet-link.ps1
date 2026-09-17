$ErrorActionPreference = 'Continue'
$log = 'C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot\tools\ethernet-fix.log'
function W($m){ $m | Tee-Object -FilePath $log -Append }
Set-Content -Path $log -Value ("=== Ethernet link fix " + (Get-Date) + " ===")

W "--- voor ---"
W (Get-NetAdapter -Name 'Ethernet' | Select-Object Name,Status,MediaConnectionState,LinkSpeed | Format-List | Out-String)

$props = @(
  @{n='Energy Efficient Ethernet'; v='Disabled'},
  @{n='Ultra Low Power Mode';      v='Disabled'},
  @{n='PCI Express Link Power Saving'; v='Disabled'},
  @{n='Idle power down restriction'; v='Enabled'}
)
foreach ($p in $props) {
  try {
    Set-NetAdapterAdvancedProperty -Name 'Ethernet' -DisplayName $p.n -DisplayValue $p.v -NoRestart -ErrorAction Stop
    W ("OK  gezet: " + $p.n + " -> " + $p.v)
  } catch { W ("SKIP " + $p.n + " : " + $_.Exception.Message) }
}

try {
  Disable-NetAdapterPowerManagement -Name 'Ethernet' -NoRestart -ErrorAction Stop
  W "OK  power management uit"
} catch { W ("SKIP power management: " + $_.Exception.Message) }

W "--- adapter herstarten ---"
Restart-NetAdapter -Name 'Ethernet' -Confirm:$false
Start-Sleep -Seconds 12

W "--- na herstart ---"
W (Get-NetAdapter -Name 'Ethernet' | Select-Object Name,Status,MediaConnectionState,LinkSpeed | Format-List | Out-String)

$state = (Get-NetAdapter -Name 'Ethernet').MediaConnectionState
if ($state -ne 'Connected') {
  W "--- geen link, forceer 100 Mbps Full Duplex ---"
  try {
    Set-NetAdapterAdvancedProperty -Name 'Ethernet' -DisplayName 'Speed & Duplex' -DisplayValue '100 Mbps Full Duplex' -NoRestart -ErrorAction Stop
    Restart-NetAdapter -Name 'Ethernet' -Confirm:$false
    Start-Sleep -Seconds 12
    W (Get-NetAdapter -Name 'Ethernet' | Select-Object Name,Status,MediaConnectionState,LinkSpeed | Format-List | Out-String)
  } catch { W ("SKIP speed/duplex: " + $_.Exception.Message) }
}

$state = (Get-NetAdapter -Name 'Ethernet').MediaConnectionState
if ($state -ne 'Connected') {
  W "--- nog geen link, terug naar Auto Negotiation ---"
  try {
    Set-NetAdapterAdvancedProperty -Name 'Ethernet' -DisplayName 'Speed & Duplex' -DisplayValue 'Auto Negotiation' -NoRestart -ErrorAction Stop
    Restart-NetAdapter -Name 'Ethernet' -Confirm:$false
    Start-Sleep -Seconds 10
  } catch { W ("SKIP herstel: " + $_.Exception.Message) }
}

W "--- DHCP vernieuwen ---"
try { Set-NetIPInterface -InterfaceAlias 'Ethernet' -Dhcp Enabled -ErrorAction Stop } catch { W $_.Exception.Message }
ipconfig /renew "Ethernet" | Out-Null
Start-Sleep -Seconds 5

W "--- eindstand ---"
W (Get-NetAdapter -Name 'Ethernet' | Select-Object Name,Status,MediaConnectionState,LinkSpeed | Format-List | Out-String)
W (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias 'Ethernet' -ErrorAction SilentlyContinue | Select-Object IPAddress,PrefixLength | Format-Table -AutoSize | Out-String)
W (Get-NetAdapterAdvancedProperty -Name 'Ethernet' | Where-Object {$_.DisplayName -match 'Energy|Ultra|Speed|PCI'} | Select-Object DisplayName,DisplayValue | Format-Table -AutoSize | Out-String)
W "=== KLAAR ==="
