# frameXTV para Android TV

Wrapper WebView a pantalla completa con navegación por foco tipo Netflix para el control remoto.

## Compilar (GitHub Actions)
1. Creá un repo **público** en GitHub (ej. `framextv-tv`) y subí todo el contenido de esta carpeta, incluido `.github/` y `app/release.keystore`.
2. Al pushear a `main` se ejecuta el workflow **Build APK**, que publica un release con `framextv.apk`.
3. URL fija a la última versión:
   `https://github.com/<TU_USUARIO>/framextv-tv/releases/latest/download/framextv.apk`

## Instalar en la tele con Downloader
1. En la tele: Play Store → instalá **Downloader** (de AFTVnews).
2. Ajustes → Apps → Acceso especial → *Instalar apps desconocidas* → habilitá **Downloader**.
   (En Fire TV: Configuración → Mi Fire TV → Opciones para desarrolladores → Apps de origen desconocido.)
3. Abrí Downloader, escribí la URL de arriba → Go → Instalar.
4. Opcional: acortá la URL en aftvnews.com/shortener para tipear un código numérico.

## Controles (estilo Netflix)
| Tecla | Acción |
|---|---|
| Flechas | Saltar entre cards / links / botones (el seleccionado se agranda y resalta, la fila scrollea sola) |
| OK | Abrir lo seleccionado (en el buscador, abre el teclado; si ya estás escribiendo, busca) |
| Atrás | Cerrar teclado / **cerrar cartel (modal)** / salir del video fullscreen / página anterior / salir |

Si aparece un cartel (cookies, aviso, publicidad), el foco queda dentro del cartel y arranca en el botón de aceptar/cerrar. Si no tiene botón, **Atrás** lo oculta. Las ventanas emergentes se bloquean.

**En el reproductor:** parado sobre el video, OK entra a los controles (play, ⚙, subtítulos). Con el video seleccionado, ←/→ adelantan o atrasan 10 s. Desde el borde de los controles salís a la página, o con **Atrás**.

**Barra superior:** se oculta y queda un botón **☰ Menú** arriba a la izquierda. Subí hasta arriba, OK abre la barra; bajando o con **Atrás** se vuelve a ocultar.

La navegación vive en `app/src/main/assets/tvnav.js` (se inyecta en cada página).

## Cambiar de sitio
Editá `HOME_URL` en `app/src/main/java/cc/framextv/tv/MainActivity.java`.
