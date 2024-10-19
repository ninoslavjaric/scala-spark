package org.nino
import net.ruippeixotog.scalascraper.model.{Element}
import net.ruippeixotog.scalascraper.browser.{JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.{elementList, element, text, texts, elements}
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.utils.Cache.File
import java.net.{URL, SocketTimeoutException, SocketException}
import org.schema.Competition
import org.schema.CompetitionHistory
import org.utils.{Proxy, UserAgent}
import org.jsoup.HttpStatusException
import net.ruippeixotog.scalascraper.model.Document
import com.typesafe.scalalogging.Logger
// import org.slf4j.LoggerFactory


object Scraper {
    private val logger = Logger(getClass)
    final val CACHE_DIR = "cache"
    final val BASE_URL = "https://fbref.com"
    // var browser = new JsoupBrowser(proxy = Proxy.getRandom(), userAgent = UserAgent.getRandom())

    def get(url: String): Document = {
        // var proxies = Proxy.getProxies.filter(proxy => proxy != browser.proxy)
        logger.info(s"Scrapint $url")
        var failedProxies: List[java.net.Proxy] = List()
        var browser = new JsoupBrowser(proxy = Proxy.getRandom, userAgent = UserAgent.getRandom)

        val fullUrl = s"${BASE_URL}${url}"
        val urlObj = new URL(fullUrl)

        val filePath = urlObj.getPath()

        if (!File.has(filePath)) {
            logger.info(s"Browser proxy ${browser.proxy.address()}")
        }

        var doc: Document = null

        def regenerateBrowser(): Unit = {
            failedProxies = browser.proxy :: failedProxies
            browser = new JsoupBrowser(proxy = Proxy.getRandom(Proxy.getProxies.diff(failedProxies)), userAgent = UserAgent.getRandom)
            logger.info(s"Swapping browser proxy ${browser.proxy.address()}")
        }

        def handleKnownException(e: Throwable, throwIt: Boolean = false): Unit = {
            logger.warn(e.getMessage())
            if (throwIt) {
                throw e
            }
            regenerateBrowser()
        }

        while (doc == null && Proxy.getProxies.diff(failedProxies).length > 0) {
            try {
                doc = if (!File.has(filePath)) browser.get(fullUrl) else browser.parseString(File.get(filePath))
            } catch {
                case e: SocketTimeoutException => handleKnownException(e)
                case e: HttpStatusException => handleKnownException(e, e.getStatusCode() != 429)
                case e: SocketException => handleKnownException(e)
                case e: javax.net.ssl.SSLException => handleKnownException(e)
                case e: java.util.NoSuchElementException => handleKnownException(e)
                case e: java.io.IOException => handleKnownException(e, !e.getMessage().toLowerCase.contains("unable to tunnel through"))
                case e: Throwable => {
                    logger.warn(e.getMessage(), e)
                    logger.warn(e.getCause().getMessage(), e.getCause())
                    sys.exit(1)
                    throw e
                }
            }
        }

        if (doc == null) {
            throw new Exception(s"doc is null and we're out of proxies")
        }
        logger.info(s"doc found $url")

        if (!File.has(filePath)) {
            File.set(filePath, doc.toHtml)
        }

        doc
    }
    /**
      * 
        <tr class="gender-m">
            <th scope="row" class="left " data-stat="league_name"><a href="/en/comps/14/history/Copa-Libertadores-Seasons">Copa Libertadores de América</a></th>
            <td class="center " data-stat="gender">M</td>
            <td class="left " data-stat="governing_body">CONMEBOL</td>
            <td class="left " data-stat="minseason" csk="2014-2014"><a href="/en/comps/14/2014/2014-Copa-Libertadores-Stats">2014</a></td>
            <td class="left " data-stat="maxseason" csk="2024-2024"><a href="/en/comps/14/Copa-Libertadores-Stats">2024</a></td>
            <td class="right " data-stat="tier">1st</td>
        </tr>
      *
      * @param node
      */
    def parseAttr(category: String)(node: Element): Competition = {
        val name = node >> element("[data-stat=league_name] > a")
        val gender = node >> text("[data-stat=gender]")
        val tier = (node >?> text("[data-stat=tier]"))
        val body = node >?> text("[body-stat=governing_body]")
        
        Competition(
            name = name.text.trim(), 
            href = name.attr("href").trim(), 
            gender = gender.trim(),
            tier = tier.getOrElse("").trim(),
            governingBody = body.getOrElse("").trim(),
        )
    }

    def parseCompetitionTables(table: Element): List[Competition] = {
        val title = table >> text("caption")
        val bodyRows = (table >> elementList("tbody tr"))
        
        logger.info(s" Parsing table $title")

        bodyRows.map(this.parseAttr(category = title))
    }

    def parseCompetitionHistoryRow(competition: Competition)(node: Element): Option[CompetitionHistory] = {
        logger.info(s"Parsing ${competition.name}")
        try {
            val year = node >> element("[data-stat=year_id] > a, [data-stat=year] > a")
            val name = node >> text("[data-stat=league_name], [data-stat=comp_name]")
            
            Some(CompetitionHistory(name = name.trim(), href = year.attr("href").trim(), season = year.text.trim(), competitionID = competition.toString))
        } catch {
            case e: java.util.NoSuchElementException => None
            case e: Throwable => throw e
        }
    }

    def getCompetitionHistoryTables(competition: Competition): List[Option[CompetitionHistory]] = 
        (get(competition.href) >> elementList("table#seasons tbody tr")).map(this.parseCompetitionHistoryRow(competition))
    
    def waitForProxies(): Unit = {
        logger.info(s"Waiting for some valid proxies to start scraping")
        while (Proxy.getProxies.length == 0) {
            Thread.sleep(1000)
        }
    }

    def scrape(args: Array[String]): Unit = {
        Proxy.startCollectingProxies()

        waitForProxies()

        val doc = this.get("/en/comps")
        val tables = doc >> elementList("table[id^=comps]")
        
        val competitions: List[Competition] = (doc >> elementList("table[id^=comps]")).map(this.parseCompetitionTables).flatten
        // val england = competitions.find(comp => comp.name == "Premier League").orNull

        val histories: List[Option[CompetitionHistory]] = competitions.map(getCompetitionHistoryTables).flatten
        
        logger.info("All good. Bue!!!")
        sys.exit(0)
    }
}
