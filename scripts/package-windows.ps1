param(
    [ValidateSet("image", "exe")]
    [string]$Formato = "image",

    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Versao = "1.0.0",

    [switch]$Console
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($env:OS -ne "Windows_NT") {
    throw "Este script deve ser executado no Windows."
}

# Diretórios
$projeto = (Resolve-Path (
    Join-Path $PSScriptRoot ".."
)).Path

$target = Join-Path $projeto "target"
$entrada = Join-Path $target "package-input"
$saida = Join-Path $target "distribution"

$nomeApp = "AutoNota"
$nomeJar = "auto_nota-1.0-SNAPSHOT.jar"

$jarPrincipal = Join-Path $target $nomeJar
$imagem = Join-Path $saida $nomeApp

# Ferramentas
$maven = (Get-Command "mvn.cmd" `
    -ErrorAction Stop).Source

$jpackage = (Get-Command "jpackage.exe" `
    -ErrorAction Stop).Source

Push-Location $projeto

try {
    Write-Host "Compilando Auto-Nota..."

    & $maven clean package

    if ($LASTEXITCODE -ne 0) {
        throw "Falha na compilacao Maven."
    }

    if (-not (Test-Path $jarPrincipal)) {
        throw "JAR principal nao encontrado."
    }

    # Copiar dependências para a distribuição
    New-Item -ItemType Directory `
        -Path $entrada `
        -Force | Out-Null

    Write-Host "Copiando dependencias..."

    & $maven dependency:copy-dependencies `
        "-DincludeScope=runtime" `
        "-DoutputDirectory=$entrada"

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao copiar dependencias."
    }

    # Copia apenas o JAR normal.
    # Nao utiliza o jar-with-dependencies.
    Copy-Item $jarPrincipal `
        (Join-Path $entrada $nomeJar)

    # Verifica se o JavaFX para Windows foi incluido.
    $javafxWindows = Get-ChildItem `
        -Path $entrada `
        -Filter "javafx-graphics-*-win.jar"

    if (-not $javafxWindows) {
        throw "JavaFX Windows nao encontrado."
    }

    # Evita incluir chaves privadas acidentalmente.
    $chavesPrivadas = Get-ChildItem `
        -Path $entrada `
        -Recurse `
        -Filter "*.pk8"

    if ($chavesPrivadas) {
        throw "Chave privada encontrada no pacote!"
    }

    # Confirma a chave publica de licenciamento.
    $recursoPublico = & jar tf $jarPrincipal |
        Select-String "licensing/auto-nota-public-key.der"

    if (-not $recursoPublico) {
        throw "Chave publica nao encontrada no JAR."
    }

    New-Item -ItemType Directory `
        -Path $saida `
        -Force | Out-Null

    # Criar a imagem executavel Windows.
    $argsImagem = @(
        "--type", "app-image",
        "--name", $nomeApp,
        "--app-version", $Versao,
        "--vendor", "AutoNota",
        "--description", "Comprovantes simulados",
        "--input", $entrada,
        "--dest", $saida,
        "--main-jar", $nomeJar,
        "--main-class", "com.tomazbr9.Launcher",
        "--add-modules",
        "java.desktop,java.sql,java.scripting,jdk.crypto.ec,jdk.charsets,jdk.localedata"
    )

    # Icone opcional
    $icone = Join-Path $projeto `
        "packaging\auto-nota.ico"

    if (Test-Path $icone) {
        $argsImagem += @("--icon", $icone)
    }

    # Console opcional para depuracao
    if ($Console) {
        $argsImagem += "--win-console"
    }

    Write-Host "Criando aplicativo Windows..."

    & $jpackage @argsImagem

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao criar imagem Windows."
    }

    $executavel = Join-Path $imagem "AutoNota.exe"

    if (-not (Test-Path $executavel)) {
        throw "Executavel nao encontrado."
    }

    Write-Host "Aplicativo criado em: $executavel"

    # Opcionalmente gerar o instalador .exe.
    if ($Formato -eq "exe") {

        $candle = Get-Command "candle.exe" `
            -ErrorAction SilentlyContinue

        $light = Get-Command "light.exe" `
            -ErrorAction SilentlyContinue

        if (-not $candle -or -not $light) {
            throw "Instale WiX Toolset 3.11 e adicione ao PATH."
        }

        Write-Host "Criando instalador Windows..."

        $argsInstalador = @(
            "--type", "exe",
            "--name", $nomeApp,
            "--app-image", $imagem,
            "--dest", $saida,
            "--app-version", $Versao,
            "--vendor", "AutoNota",
            "--win-menu",
            "--win-menu-group", "AutoNota",
            "--win-shortcut",
            "--win-upgrade-uuid",
            "fea266c4-8a4f-4fc7-bb2c-51f064d074ba"
        )

        & $jpackage @argsInstalador

        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao gerar instalador."
        }

        Write-Host "Instalador gerado em: $saida"
    }

    Write-Host "Processo concluido!"

} finally {
    Pop-Location
}
