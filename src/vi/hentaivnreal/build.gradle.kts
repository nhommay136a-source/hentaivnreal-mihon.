import io.github.keiyoushi.gradle.api.ContentWarning
plugins { alias(kei.plugins.extension) }
keiyoushi {
    name = "HentaiVNReal"
    versionCode = 1
    contentWarning = ContentWarning.NSFW
    libVersion = "1.6"
    source {
        name = "HentaiVNReal"
        lang = "vi"
        baseUrl = "https://hentaivnreal.com"
    }
}
