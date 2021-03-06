package com.sismics.reader.rest;

import com.sismics.reader.rest.filter.CookieAuthenticationFilter;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Exhaustive test of the all resource.
 * 
 * @author jtremeaux
 */
public class TestAllResource extends BaseJerseyTest {
    /**
     * Test of the all resource.
     * 
     * @throws JSONException
     */
    @Test
    public void testAllResource() throws JSONException {
        // Create user all1
        clientUtil.createUser("all1");
        String all1AuthToken = clientUtil.login("all1");

        // Subscribe to korben.info
        WebResource subscriptionResource = resource().path("/subscription");
        subscriptionResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        MultivaluedMapImpl postParams = new MultivaluedMapImpl();
        postParams.add("url", "http://localhost:9997/http/feeds/korben.xml");
        ClientResponse response = subscriptionResource.put(ClientResponse.class, postParams);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        JSONObject json = response.getEntity(JSONObject.class);
        String subscription0Id = json.optString("id");
        Assert.assertNotNull(subscription0Id);
        
        // Check the category tree
        subscriptionResource = resource().path("/category");
        subscriptionResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = subscriptionResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        JSONArray categories = json.optJSONArray("categories");
        Assert.assertNotNull(categories);
        Assert.assertEquals(1, categories.length());
        JSONObject rootCategory = categories.optJSONObject(0);
        String rootCategoryId = rootCategory.optString("id");
        Assert.assertNotNull(rootCategoryId);
        categories = rootCategory.optJSONArray("categories");
        Assert.assertEquals(0, categories.length());

        // Check the root category
        WebResource categoryResource = resource().path("/category/" + rootCategoryId);
        categoryResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = categoryResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        JSONArray articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(10, articles.length());
        JSONObject article = (JSONObject) articles.get(1);
        String article1Id = article.getString("id");
        article = (JSONObject) articles.get(2);
        String article2Id = article.getString("id");

        // Check pagination
        categoryResource = resource().path("/category/" + rootCategoryId);
        categoryResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        MultivaluedMapImpl queryParams = new MultivaluedMapImpl();
        queryParams.add("after_article", article1Id);
        response = categoryResource.queryParams(queryParams).get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(8, articles.length());
        Assert.assertEquals(article2Id, article.getString("id"));

        // Check the all resource
        WebResource allResource = resource().path("/all");
        allResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = allResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(10, articles.length());
        article = (JSONObject) articles.get(1);
        article1Id = article.getString("id");
        article = (JSONObject) articles.get(2);
        article2Id = article.getString("id");

        // Check pagination
        categoryResource = resource().path("/all");
        categoryResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        queryParams = new MultivaluedMapImpl();
        queryParams.add("after_article", article1Id);
        response = categoryResource.queryParams(queryParams).get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(8, articles.length());
        Assert.assertEquals(article2Id, article.getString("id"));

        // Marks all articles as read
        allResource = resource().path("/all/read");
        allResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = allResource.post(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));

        // Check the all resource
        allResource = resource().path("/all");
        allResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = allResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(10, articles.length());

        // Check in the subscriptions that there are no unread articles left
        subscriptionResource = resource().path("/subscription");
        subscriptionResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        response = subscriptionResource.queryParams(queryParams).get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        Assert.assertEquals(0, json.optInt("unread_count"));
        categories = json.getJSONArray("categories");
        rootCategory = categories.getJSONObject(0);
        JSONArray subscriptions = rootCategory.getJSONArray("subscriptions");
        JSONObject subscription0 = subscriptions.getJSONObject(0);
        Assert.assertEquals(0, subscription0.optInt("unread_count"));

        // Check the all resource for unread articles
        allResource = resource().path("/all");
        allResource.addFilter(new CookieAuthenticationFilter(all1AuthToken));
        queryParams = new MultivaluedMapImpl();
        queryParams.add("unread", true);
        response = allResource.queryParams(queryParams).get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(0, articles.length());
    }

    @Test
    public void testMultipleUsers() throws JSONException {
        // Create user multiple1
        clientUtil.createUser("multiple1");
        String multiple1AuthToken = clientUtil.login("multiple1");

        // Subscribe to korben.info
        WebResource subscriptionResource = resource().path("/subscription");
        subscriptionResource.addFilter(new CookieAuthenticationFilter(multiple1AuthToken));
        MultivaluedMapImpl postParams = new MultivaluedMapImpl();
        postParams.add("url", "http://localhost:9997/http/feeds/korben.xml");
        ClientResponse response = subscriptionResource.put(ClientResponse.class, postParams);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        JSONObject json = response.getEntity(JSONObject.class);
        String subscription0Id = json.optString("id");
        Assert.assertNotNull(subscription0Id);
        
        // Check the all resource
        WebResource allResource = resource().path("/all").queryParam("unread", "true");
        allResource.addFilter(new CookieAuthenticationFilter(multiple1AuthToken));
        response = allResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        JSONArray articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(10, articles.length());

        // Create user multiple2
        clientUtil.createUser("multiple2");
        String multiple2AuthToken = clientUtil.login("multiple2");

        // Subscribe to korben.info (alternative URL)
        subscriptionResource = resource().path("/subscription");
        subscriptionResource.addFilter(new CookieAuthenticationFilter(multiple2AuthToken));
        postParams = new MultivaluedMapImpl();
        postParams.add("url", "http://localhost:9997/http/feeds/korben2.xml");
        response = subscriptionResource.put(ClientResponse.class, postParams);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        subscription0Id = json.optString("id");
        Assert.assertNotNull(subscription0Id);
        
        // Check the all resource
        allResource = resource().path("/all").queryParam("unread", "true");
        allResource.addFilter(new CookieAuthenticationFilter(multiple2AuthToken));
        response = allResource.get(ClientResponse.class);
        Assert.assertEquals(Status.OK, Status.fromStatusCode(response.getStatus()));
        json = response.getEntity(JSONObject.class);
        articles = json.optJSONArray("articles");
        Assert.assertNotNull(articles);
        Assert.assertEquals(10, articles.length());
    }
}