package eu.kanade.tachiyomi.extension.vi.hentaivnreal

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.utils.asJsoup
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document

@Source
abstract class HentaiVNReal : KeiSource() {
    override suspend fun getPopularManga(page: Int): MangasPage {
        val url = if (page == 1) baseUrl else "$baseUrl/?page=$page"
        return client.get(url, fetch = Fetch.DOCUMENT).asJsoup().toMangasPage()
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        client.get("$baseUrl/?order=update&page=$page", fetch = Fetch.DOCUMENT).asJsoup().toMangasPage()

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val url = "$baseUrl/".toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .addQueryParameter("page", page.toString())
            .build()
        return client.get(url, fetch = Fetch.DOCUMENT).asJsoup().toMangasPage()
    }

    private fun Document.toMangasPage(): MangasPage {
        val entries = select("a[href*=/truyen/]")
            .filter { it.selectFirst("img") != null }
            .distinctBy { it.absUrl("href") }
        val mangas = entries.mapNotNull { element ->
            val href = element.absUrl("href")
            val title = element.attr("title").ifBlank {
                element.selectFirst("img")?.attr("alt").orEmpty().ifBlank { element.text() }
            }.trim()
            if (href.isBlank() || title.isBlank()) return@mapNotNull null
            SManga.create().apply {
                setUrlWithoutDomain(href)
                this.title = title
                thumbnail_url = element.selectFirst("img")?.let { img ->
                    img.absUrl("data-src").ifBlank { img.absUrl("src") }
                }?.takeIf(String::isNotBlank)
            }
        }
        val next = selectFirst("a[rel=next], .pagination a.next, a:contains(›), a:contains(Tiếp)") != null
        return MangasPage(mangas, next)
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != baseUrl.toHttpUrl().host) return null
        val doc = client.get(url, fetch = Fetch.DOCUMENT).asJsoup()
        val title = doc.selectFirst("h1")?.text()
            ?: doc.selectFirst("title")?.text()?.substringBefore(" - ") ?: return null
        return SManga.create().apply {
            setUrlWithoutDomain(url.toString())
            this.title = title
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst("img")?.absUrl("src")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
                ?: doc.selectFirst(".description, .entry-content")?.text()
            genre = doc.select(".genre a, a[rel=tag]").eachText().joinToString()
            author = doc.select(".author a, .author").firstOrNull()?.text()
            status = SManga.UNKNOWN
            initialized = true
        }
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean,
    ): SMangaUpdate {
        val doc = client.get(getMangaUrl(manga), fetch = Fetch.DOCUMENT).asJsoup()
        val updated = manga.apply {
            doc.selectFirst("h1")?.text()?.let { title = it }
            doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?.takeIf(String::isNotBlank)?.let { thumbnail_url = it }
            doc.selectFirst("meta[name=description]")?.attr("content")
                ?.takeIf(String::isNotBlank)?.let { description = it }
            genre = doc.select(".genre a, a[rel=tag]").eachText().joinToString().ifBlank { genre }
            initialized = true
        }
        val chapterLinks = doc.select("a[href]").filter { el ->
            val href = el.absUrl("href")
            href.startsWith(baseUrl) &&
                (href.contains("chap", true) || el.text().contains("chap", true) ||
                    el.text().contains("chapter", true) || el.text().contains("chương", true))
        }.distinctBy { it.absUrl("href") }
        val parsed = chapterLinks.mapIndexed { i, el ->
            SChapter.create().apply {
                setUrlWithoutDomain(el.absUrl("href"))
                name = el.text().ifBlank { "Chapter ${i + 1}" }
                chapter_number = Regex("""(?i)(?:chap(?:ter)?|chương)\s*([0-9]+(?:\.[0-9]+)?)""")
                    .find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
            }
        }.asReversed()
        return SMangaUpdate(updated, parsed)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val doc = client.get(getChapterUrl(chapter), fetch = Fetch.DOCUMENT).asJsoup()
        val images = doc.select(".reading-content img, .chapter-content img, .reader img, #chapter-content img, article img")
            .mapNotNull { img ->
                img.absUrl("data-src").ifBlank {
                    img.absUrl("data-original").ifBlank { img.absUrl("src") }
                }.takeIf { it.startsWith("http") }
            }.distinct()
        if (images.isEmpty()) throw Exception("Không tìm thấy ảnh chương; trang có thể đã đổi cấu trúc hoặc yêu cầu xác minh.")
        return images.mapIndexed { i, image -> Page(i, chapter.url, image) }
    }

    // Mihon stores source URLs without the domain. OkHttp requires an absolute URL.
    private fun absoluteUrl(url: String): String =
        baseUrl.toHttpUrl().resolve(url)?.toString() ?: url

    override fun getMangaUrl(manga: SManga): String = absoluteUrl(manga.url)
    override fun getChapterUrl(chapter: SChapter): String = absoluteUrl(chapter.url)
}
