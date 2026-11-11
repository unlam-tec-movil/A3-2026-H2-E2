# Configurar Android Studio y ktlint (cada integrante)

## 1. Configurar Android Studio

1. **Settings → Editor → Code Style:** tildá *Enable EditorConfig support*.
2. En **Kotlin**, elegí *Set from… → Kotlin style guide*.
3. **Settings → Tools → Actions on Save:** tildá *Reformat code* y *Optimize imports*. Así se
   indenta solo cada vez que guardás.
4. **Settings → Plugins:** instalá «Ktlint» (lo pide el README).
5. En las opciones del plugin, activá *Distract free*: corrige lo que puede al guardar y marca el
   resto en el editor.

## 2. ¿Mi agente puede correr los checks?

- Sí, no hace falta nada nuevo.
- `AGENTS.md` ya le pide correr `ktlintFormat` cuando termina una tarea.
- El paso 5 del plan dice que se corra antes del PR.

## 3. ¿Cómo reviso ktlint a mano antes de subir mi commit?

**Opción A: automático en cada commit** (se hace una vez por computadora)

Abrí PowerShell en la carpeta del proyecto y ejecutá:

```powershell
./gradlew addKtlintFormatGitPreCommitHook
```

Desde ahí, cada `git commit` corre `ktlintFormat` sobre los archivos modificados. Hay que hacerlo en
cada computadora, porque los hooks no se suben al repo.

**Opción B: a mano** (lo haces vos)

```powershell
./gradlew ktlintFormat   # corrige el formato antes del commit
./gradlew ktlintCheck    # verifica que pase
```
