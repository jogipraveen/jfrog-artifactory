/***
  Script is used to delete the artifacts based on repo and date.
  For example: artifactory.groovy war-dev 2015-10-10
  script used to delete all files in war-dev repo older than 2015-10-10
  Replace artifactory_server, user, password with your details
***/

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.6')
import groovy.lang.Binding;
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import org.apache.http.conn.HttpHostConnectException

String repo = this.args[0] //specify in which repo do you want to delete the artifacts
String war = this.args[1]  //war file regex
String repo_date = this.args[2] + "T00:00:00.000Z" //Specify date
String mat = '$match'
String ll = '$lt'

def query="""items.find({"type" : "file","repo" :{"$mat" : "${repo}"},"name" :{"$mat" : "${war}*"},"created":{"$ll": "$repo_date"}})""" //AQL query
def artifactoryURL = 'http://<artifactory_server>.com:8081/artifactory/' // Artifactory server
def restClient = new RESTClient(artifactoryURL)
restClient.setHeaders(['Authorization': 'Basic ' + "<user>:<password>".getBytes('iso-8859-1').encodeBase64()])
def dryRun = false //set the value to false if you want the script to actually delete the artifacts
def itemsToDelete = getAqlQueryResult(restClient, query)

if (itemsToDelete != null && itemsToDelete.size() > 0) 
{
    delete(restClient, itemsToDelete, dryRun)
} 
else 
{
    println('Nothing to delete')
}

/*Send the AQL to Artifactory and collect the response.*/

public List getAqlQueryResult(RESTClient restClient, String query) 
{
    def response
    try 
    {
        response = restClient.post(path: 'api/search/aql',
        body: query,
        requestContentType: 'text/plain'
        ) 
    } 
    catch (Exception e) 
    {
          println(e.message)
    }
    if (response != null && response.getData()) 
    {
        def results = [];
        response.getData().results.each {
        results.add(constructPath(it))
        }
        return results;
    } else return null
}

/* Construct the full path form the returned items.
   If the path is '.' (file is on the root) we ignores it and construct the full path from the repo and the file name only */
public constructPath(Map item) 
{
    if (item.path.toString().equals(".")) 
    {
        return item.repo + "/" + item.name
    }
    return item.repo + "/" + item.path + "/" + item.name
}


/* Send DELETE request to Artifactory for each one of the returned items */
public delete(RESTClient restClient, List itemsToDelete, def dryRun) 
{
		       
    dryMessage = (dryRun) ? "*** This is a dry run ***" : "";
    itemsToDelete.each {
    println("Trying to delete artifact: '$it'. $dryMessage")
    try 
    {
        if (!dryRun) 
	{
            restClient.delete(path: it)
        }
        println("Artifact '$it' has been successfully deleted. $dryMessage")
    } 
    catch (HttpResponseException e) 
    {
        println("Cannot delete artifact '$it': $e.message" +
        ", $e.statusCode")
    } 
    catch (HttpHostConnectException e) 
    {
        println("Cannot delete artifact '$it': $e.message")
    }
  }
}
