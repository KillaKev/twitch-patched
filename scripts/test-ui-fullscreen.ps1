[CmdletBinding()]
param([string] $Bundle, [string] $Serial)
& "$PSScriptRoot/verification/RunAndroidProbe.ps1" -Source "$PSScriptRoot/verification/UiFullscreenWindowProbe.java" -Bundle $Bundle -Serial $Serial
