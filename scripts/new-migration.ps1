param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9]+(?:_[a-z0-9]+)*$')]
    [string]$Description
)

$migrationDirectory = [IO.Path]::GetFullPath(
    (Join-Path $PSScriptRoot '..\src\main\resources\db\migration')
)

try {
    $vietnamTimeZone = [TimeZoneInfo]::FindSystemTimeZoneById('Asia/Ho_Chi_Minh')
} catch {
    $vietnamTimeZone = [TimeZoneInfo]::FindSystemTimeZoneById('SE Asia Standard Time')
}

$vietnamNow = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $vietnamTimeZone)
$version = $vietnamNow.ToString('yyyyMMddHHmmss')
$fileName = 'V' + $version + '__' + $Description + '.sql'
$migrationPath = Join-Path $migrationDirectory $fileName

if (Test-Path -LiteralPath $migrationPath) {
    throw ('Migration version ' + $version + ' already exists. Wait one second and run the command again.')
}

$null = New-Item -ItemType File -Path $migrationPath -ErrorAction Stop
Write-Output ('Created src/main/resources/db/migration/' + $fileName)
