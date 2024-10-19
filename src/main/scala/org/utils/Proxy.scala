package org.utils

import net.ruippeixotog.scalascraper.browser
import scala.util.{Random, Success, Failure}
import java.net.{InetSocketAddress, Proxy => JavaProxy}

// import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

import sttp.client3.quick._
import sttp.client3.Response

import scala.io.Source
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetAddress
import java.util.concurrent.Executors

import scala.sys.process._
import org.nino.Scraper
import com.typesafe.scalalogging.Logger

import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import org.w3c.dom.{Document, Element}
import schema.ProxySource

object Proxy {
    private val logger = Logger(getClass)
    private var proxyMap: Map[String, List[JavaProxy]] = Map()
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

    def getProxyMap(): Map[String, List[JavaProxy]] = proxyMap

    def startCollectingProxies(): Future[Unit] = Future {
        logger.info("Collecting proxies")

        val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        val builder: DocumentBuilder = factory.newDocumentBuilder()
        val document: Document = builder.parse(getClass.getClassLoader.getResourceAsStream("proxy-urls.xml"))

        document.getDocumentElement().normalize()

        val sourceNodes = document.getElementsByTagName("source")

        (0 until sourceNodes.getLength).toList.map(id => sourceNodes.item(id).getAttributes())
            .foreach(item => grabProxyInstances(new ProxySource(
                item.getNamedItem("url").getNodeValue(), 
                item.getNamedItem("type").getNodeValue().trim() match {
                    case "JavaProxy.Type.HTTP" => JavaProxy.Type.HTTP
                    case "JavaProxy.Type.SOCKS" => JavaProxy.Type.SOCKS
                    case _ => throw new Exception(s"Wrong type set ${item.getNamedItem("type").getNodeValue().trim()}")
                }, 
                if (item.getNamedItem("extra") == null) null else item.getNamedItem("extra").getNodeValue()
            )))
        // grabProxyInstances("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt", JavaProxy.Type.HTTP)
        // grabProxyInstances("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks4.txt", JavaProxy.Type.SOCKS, "--socks4")
        // grabProxyInstances("https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt", JavaProxy.Type.SOCKS, "--socks5")
    }

    def grabProxyInstances(source: ProxySource): Future[Unit] = (
        if (source.extra == null) grabProxyInstances(source.url, source.typeInput) 
        else grabProxyInstances(source.url, source.typeInput, source.extra)
    )

    def grabProxyInstances(url: String, typeInput: JavaProxy.Type): Future[Unit] = Future(grabProxyInstances(url, typeInput, ""))
    

    def proxyHealthCheckerCallback(url: String, typeInput: JavaProxy.Type, proxyUrl: String): Future[Unit] = Future[Unit] {
        var lista: List[JavaProxy] = proxyMap.get(url).getOrElse(List())
        try {
            val baseUrl = Scraper.BASE_URL
            logger.debug(s"$proxyUrl [curl -s --connect-timeout 10 -x $proxyUrl -s -o /dev/null -w '%{http_code}' $baseUrl]")
            val http_code = s"curl -s --connect-timeout 3 -x $proxyUrl -s -o /dev/null -w '%{http_code}' $baseUrl".!!.trim()
            logger.info(s"$proxyUrl code $http_code")

            if (http_code.toInt > 399) {
                throw new Exception("No no no")
            }

            val Array(ip, port) = proxyUrl.split(":")
            lista = new JavaProxy(typeInput, new InetSocketAddress(ip, port.toInt)) :: lista 
            proxyMap = proxyMap + (url -> lista)
        } catch {
            case e: RuntimeException => {
                logger.debug(e.getMessage())
            }
            case e: Throwable => {
                logger.error(e.getMessage())
                throw e
            }
        }
    }
    

    def grabProxyInstances(url: String, typeInput: JavaProxy.Type, extra: String): Future[Unit] = Future {
        logger.info(s"Collecting proxies $url")
        var lista: List[JavaProxy] = proxyMap.get(url).getOrElse(List())

        if (lista.isEmpty) {
            val connection: HttpURLConnection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
            connection.setRequestMethod("GET")
            val futures: List[Future[Unit]] = Source.fromInputStream(connection.getInputStream())
                .getLines().map(inputLine => proxyHealthCheckerCallback(url, typeInput, inputLine)).toList
        }
    }

    def getProxies(): List[JavaProxy] = proxyMap.values.flatten.toList
    
    def getRandom(): JavaProxy = 
        getRandom(getProxies)

    def getRandom(availableProxies: List[JavaProxy]): JavaProxy = {
        val random = new Random()
        val options = availableProxies
        options(random.nextInt(options.length))
    } 
}

