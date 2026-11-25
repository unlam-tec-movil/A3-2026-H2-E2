package com.trazo.app.data.tiles

/**
 * Arma la URL de un tile a partir de la plantilla «PNG tile URL» que entrega CARTO (con la API key
 * incluida). Reemplaza los marcadores `{z}`, `{x}`, `{y}`, `{s}` (subdominio) y `{r}` (`@2x`).
 * Si la plantilla no pide tiles `@2x`, los agrega: el mapa usa tiles de 512 px.
 */
object CartoTileUrl {
    /** URL pública sin clave: CARTO la sirve con un aviso de que falta la API key. */
    const val PUBLIC_TEMPLATE = "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"

    private const val RETINA_SUFFIX = "@2x"
    private val subdomains = charArrayOf('a', 'b', 'c', 'd')

    fun build(
        template: String,
        row: Int,
        col: Int,
        zoom: Int,
    ): String {
        val effectiveTemplate = withRetina(template.trim().ifEmpty { PUBLIC_TEMPLATE })
        val subdomain = subdomains[(row + col).mod(subdomains.size)]
        return effectiveTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", col.toString())
            .replace("{y}", row.toString())
            .replace("{s}", subdomain.toString())
            .replace("{r}", RETINA_SUFFIX)
    }

    private fun withRetina(template: String): String =
        if ("{r}" in template || RETINA_SUFFIX in template) {
            template
        } else {
            template.replace("{y}.png", "{y}{r}.png")
        }
}
