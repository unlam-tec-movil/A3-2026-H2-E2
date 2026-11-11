# Comandos Git Importantes

## 1. Actualización de rama/branch

Cada vez que suban cambios al repositorio y sean mergeadas, deben hacer lo siguiente → Abrir la
terminal → *Git Bash* → *ejecutar los siguientes comandos*.

1. Cambiar a la rama develop → git switch develop
2. Actualizar tu rama develop local → git pull origin develop
3. Cambiar a la rama en la cual estas trabajando o una nueva → git switch <<nombre-de-tu-rama>>
4. Fusionas los cambios de develop con tu rama → git merge develop

Si la rama en la cual estabas trabajando tenia cambios en la carpeta /app o en dependencias,
entonces es posible que tengas que lidiar con merge conflicts.

## 2. Borrar marca de Claude en un commit/s

Si al subir tu rama al repositorio, notaste el icono de Claude y queres sacarlo (ocultarlo) para que
no se interponga en el resultado de tus contribuciones seguí estos pasos.

1. Abri el historial interactivo → git rebase -i

Se abrirá un editor de texto en la terminal o en vscode con una lista de tus commits. Verás la
palabra pick al lado de cada uno.

2. Buscá el commit que querés modificar.
3. Cambiá la palabra pick por reword (o solo la letra r) manualmente.
4. Guardá y cerrá el editor (si es el editor Vim, presioná Esc, escribí :wq y dale Enter).
5. Escribí el nuevo mensaje: Git se detendrá en ese commit y abrirá otro editor para que escribas el
   mensaje nuevo. Solamente vas a borrar esta linea del mensaje: **Co-Authored-By: Claude Sonnet
   5.5 <noreply@anthropic.com>** guardá y cerrá.

Realiza este paso por cada commit y listo!. 

