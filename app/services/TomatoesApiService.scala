package services
import play.api.Play
import play.api.libs.ws.WS
import play.api.Logger
import models.helper.TomatoesSearch
import scala.collection.JavaConversions._
import models.helper.TomatoesSearch
import com.codahale.jerkson.Json._
import models.helper.TomatoesSearch

object TomatoesApiService {
  
  private lazy val apikey = Play.current.configuration.getString("tomatoes.apikey")
    .getOrElse(Option(System.getenv("TOMATOES_API_KEY"))
      .getOrElse(throw new IllegalStateException("No tomatoes api key defined")))
  
  def find(title:String, year:Option[Int] = None) = {
    val url = "http://api.rottentomatoes.com/api/public/v1.0/movies.json"
    val q = title + year.map(" " + _).getOrElse("")
    val response = WS.url(url).withQueryString("q" -> q, "apikey" -> apikey).get()
    response.map { response =>
      val json = response.json
      val error = (json \ "error").asOpt[String]
      if (error.isDefined) {
        Logger.warn("Tomatoes api error: " + error.get)
        println(response.body)
        None
      } else {
        val search = parse[TomatoesSearch](response.body)
        val movies = search.movies
        // putting year in the query does not seem to return the correct movie as first item
        if (movies.isEmpty) {
          None
        } else {
          // we probably want to be a bit smarter and use some Levenshtein distance lib
          // http://en.wikipedia.org/wiki/Approximate_string_matching
          val exact = movies.filter(_.title.equalsIgnoreCase(title))
          yearMatchIfExists(year, exact).orElse(yearMatchIfExists(year, movies))
        }
      }
    }
  }
  
  def getById(id:String) = {
    //http://api.rottentomatoes.com/api/public/v1.0/movies/13863.json
  }

  private def yearMatchIfExists(year: Option[Int], movies: List[models.helper.TomatoesMovie]) = {
    year.map { y =>
      movies.filter(m => (m.year.getOrElse(Int.MinValue) == y)).headOption.orElse(movies.headOption)
    } getOrElse {
      movies.headOption
    }
  }
  
  

}