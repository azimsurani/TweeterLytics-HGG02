package controllers;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;

import models.Tweet;
import models.TweetSearchResult;
import play.cache.AsyncCacheApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.*;
import services.TweetService;
import utils.Util;
import java.util.Optional;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.ArrayList;
import java.util.List;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

	private HttpExecutionContext ec;

	private AsyncCacheApi cache;

	@Inject
	public HomeController(HttpExecutionContext ec,AsyncCacheApi cache) {
		this.ec = ec;
		this.cache = cache;	
	}



	/**
	 * An action that renders an HTML page with a welcome message.
	 * The configuration in the <code>routes</code> file means that
	 * this method will be called when the application receives a
	 * <code>GET</code> request with a path of <code>/</code>.
	 */
	public Result index() {
		
		cache.removeAll();

		return ok(views.html.index.render());
	}

	public CompletionStage<Result> getTweetsBySearch(String keyword){

		return supplyAsync(()->{

			CompletionStage<Optional<List<Tweet>>> cachedTweets = cache.get(keyword.toLowerCase());
			
			List<Tweet> tweets = new ArrayList<>();
			
			TweetSearchResult response = null;

			try {
				
				if(cachedTweets.toCompletableFuture().get().isPresent()) {

					tweets = cachedTweets.toCompletableFuture().get().get();

				}

				else {

					tweets = TweetService.searchForKeywordAndGetTweets(keyword);
					
					cache.set(keyword.toLowerCase(), tweets);
					
				}
				
				//Sentiment Analysis Code
				
				response = new TweetSearchResult(keyword, tweets.subList(0, tweets.size() < 10 ? tweets.size() : 10) , "neutral");

				
			} catch (InterruptedException|ExecutionException e) {
				
				e.printStackTrace();
				
			}

			JsonNode jsonObjects = Json.toJson(response);

			return ok(Util.createResponse(jsonObjects, true));

		},ec.current());

	}

}
