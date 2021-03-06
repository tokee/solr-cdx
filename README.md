# solr-cdx

Solr schema and helper tools for providing CDX lookups using Solr.

## Status and Limitations

 * The current scope is proof of concept
 * CDX Server API-support is extremely preliminary
 * Feature tested with sample data from archive.org and 20M CDX-entries from the Danish Net Archive
 * Not scale tested

### Standard Solr goodies

 * Iterative updates: New CDX-data are simply added to the existing ones without any requirements of re-calculation of existing data or similar large overheads
 * Sharding: SolrCloud makes it possible to treat different Solr indexes (shards) as a single one. Growing beyond a single machine is easy
 * Replication: Redundancy and throughput scaling is build into SolrCloud 

## So what is the idea?

[CDX-files](https://archive.org/web/researcher/cdx_file_format.php) are used for mapping URLs, timestamps and other meta-data to WARC filenames and offsets. They are also used for extracting some statistics.

Seen from my point of view (I do a lot of search-stuff, primarily with Solr), there are three parts here

1. Limit the amount of meta-data to get. This can be a query for a specific URL at a specific time. Or within a time-range. Or from a specific domain. Or a specific mime-type. Or any combination hereof.
2. Optionally aggregate the response. This can be a simple count of matches. Or grouping by uniqueness (hash) or WARC file name. Or faceting on year or domain.
3. Deliver the response. This can be a single WARC file name + offset. Or 10 billion URLs. Either paged in chunks or as a single stream of data.

Interestingly enough, Solr seems to fit well into this. Extremely well, I would say, but then I am quite biased here. Let's pretend we have a (yet fictional) solr-cdx running and discuss the [CDX Server Requirements](https://github.com/iipc/openwayback/wiki/CDX-Server-requirements):

1. _The user has a link to a particular version of a document:_ This is an exact lookup and can be handled with `query=url:"example.com/kittens.html" AND date:"2016-03-04T20:54:10Z"`.
   * Sample: [http://localhost...?q=url:"ar,com,adscl...09408_1_small.jpg AND date:"2011-02-25T19:03:07Z"...](http://localhost:8983/solr/cdx/select?q=url%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%29%2Fpublicacion%2Fimages%2F209408_1_small.jpg%22+AND+date%3A%222011-02-25T19%3A03%3A07Z%22&wt=json&indent=true)
2. _The user selects one particular capture in the calendar:_ This relaxes timestamp-matching to the one closest in time: `query=url:"example.com/kittens.html" & sort=abs(sub(ms(2016-03-04T20:54:10Z), date)) asc` 
   * Sample: [http://localhost...?q=url:"ar,com,adscl...09408_1_small.jpg"&sort=abs(sub(ms(2011-02-25T19_03:07Z), date)) asc...](http://localhost:8983/solr/cdx/select?q=url%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%29%2Fpublicacion%2Fimages%2F209408_1_small.jpg%22&sort=abs%28sub%28ms%282011-02-25T19%3A03%3A07Z%29%2C+date%29%29+asc&wt=json&indent=true)
3. _Get the best matching page when following a link:_ Same as above.
4. _Get the best match for embedded resources:_ Same as above.
5. _User requests/searches for an exact url without any timestamp, expecting to get a summary of captures for the url over time:_ This is just a question of what the result set is. `query=url:"example.com/kittens.html" & df=date`
   * Sample: [http://localhost...?q=url:"ar,com,adscl...09408_1_small.jpg"&fl=date...](http://localhost:8983/solr/cdx/select?q=url%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%29%2Fpublicacion%2Fimages%2F209408_1_small.jpg%22&fl=date&wt=json&indent=true)
6. _User looks up a domain expecting a summary of captures over time:_ Slight variation of above. `query=domain:"example.com" & fl=date` or if domain is not indexed explicitly: `query=url:example.com/* & fl=date`.
   * Sample: [http://localhost...?q=sdomain:"ar,com,adscl...&fl=date...](http://localhost:8983/solr/cdx/select?q=sdomain%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%22&fl=date&wt=json&indent=true)
7. _User searches with a truncated path expecting the results to show up as matching paths regardless of time:_ `query=url:example.com/kit* & fl=url`.
   * Sample: [http://localhost...?q=url:ar,com,adscl...09408_1_s*&fl=url,date...](http://localhost:8983/solr/cdx/select?q=url%3Aar%2Ccom%2Cadsclasificados%2Caimogasta%5C%29%2Fpublicacion%2Fimages%2F209408_1_s*&fl=url%2Cdate&wt=json&indent=true)
8. _User searches with a truncated path expecting the results to show up as matching paths regardless of time and subdomain:_ It could be handled effectively by having the root domain and path explicitly `query=root_domain:example.com AND path=kit* & fl=url` or as a (very heavy) regexp `query=/.*[.]example.com/kit.*/ & fl=url`. With [SURT](http://crawler.archive.org/apidocs/org/archive/util/SURT.html) URLs a regexp is somewhat lighter as only the URLs with the domain are processed, but it is still heavy. Having a (SURT) domain-field and a path field seems like the way to go here.
   * Sample: [http://localhost...?q=sdomain:ar,com,adsclasificados,* AND path:publicacion/images/209408_1_sm*&fl=url,date...](http://localhost:8983/solr/cdx/select?q=sdomain%3Aar%2Ccom%2Cadsclasificados%2C*+AND+path%3Apublicacion%2Fimages%2F209408_1_sm*&fl=url,date&wt=json&indent=true)
9. _User navigates back and forth in the calendar:_ This is a range-limit: `query=url:"example.com/kittens.html AND timestamp:["2014-01-01T00:00:00Z" TO 2015-12-31T23:59:59Z"" & fl=date`.
   Sample: [http://localhost...?q=url:"ar,com,adscl...09408_1_small.jpg" AND date:["2010-01-01T00:00:00Z" TO "2010-01-01T00:00:00Z"]&fl=date](http://localhost:8983/solr/cdx/select?q=url%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%29%2Fpublicacion%2Fimages%2F209408_1_small.jpg%22+AND+date%3A%5B%222010-01-01T00%3A00%3A00Z%22+TO+%222011-12-31T23%3A59%3A59Z%22%5D&fl=date&wt=json&indent=true)
10. _User wants to see when content of a page has changed:_ Grouping on digest (hash of content) and displaying the first result for each group does this. `query=url:"example.com/kittens.html & group=true & group.field=digest & sort=date asc & fl=date`.
   * Sample: [http://localhost...?q=url:"ar,com,adscl...09408_1_small.jpg" AND date:["2010-01-01T00:00:00Z" TO "2010-01-01T00:00:00Z"]&sort=date asc&fl=date&group=true&group.field=newdigest...](http://localhost:8983/solr/cdx/select?q=url%3A%22ar%2Ccom%2Cadsclasificados%2Caimogasta%29%2Fpublicacion%2Fimages%2F209408_1_small.jpg%22+AND+date%3A%5B%222010-01-01T00%3A00%3A00Z%22+TO+%222011-12-31T23%3A59%3A59Z%22%5D&sort=date+asc&fl=date&wt=json&indent=true&group=true&group.field=newdigest)
11. _User requests/searchers for an exact url with a partial timestamp, expecting to get a summary of captures for the url over time:_ This is the same as the range-limit in #9, only with finer granularity.
12. _Get a random page within a partial timestamp:_ Select a random point in time within the wanted range and return the resource harvested closest to that time, basically combining #2 and #9.
13. _Get number of snapshots taken for a date range:_ This is the same as #9, just with a hitcount and no data returned: `query=url:"example.com/kittens.html AND timestamp:["2014-01-01T00:00:00Z" TO 2015-12-31T23:59:59Z"" & rows=0`.
14. _Bulk/batch requests:_ Solr 4 supports paging and Solr 5+ supports streaming exports; see [Exporting Result sets](Bulk/batch requests). One aber dabei is that parallel requests of chunks is tricky. That might require some intermediate step where the cursorMarks are determined up front.
15. _Lookup url with specific schema:_ A question of whether or not the schema is indexed.
16. (W)ARC file management
   * _Identify the (W)ARC files which matches a query, returning a count of the query matches for each (W)ARC file:_ This can be handled by faceting: `query=url:example.com/kit* & facet=true & facet.field=arc & facec.limit=-1`.
      * Sample: [http://localhost...?q=sdomain:ar,com,adsclasificados,*&rows=0&facet=true&facet.field=arc&facet.limit=-1](http://localhost:8983/solr/cdx/select?q=sdomain%3Aar%2Ccom%2Cadsclasificados%2C*&sort=date+asc&rows=0&wt=json&indent=true&facet=true&facet.field=arc&facet.limit=-1)
   * _Given a (W)ARC file identifier, list the URLs it holds which match a set of criteria:_ This is a limiter: `query=url:example.com/kit* AND warc=myharvest20160304_2132.warc & fl=url`.
      * Sample: [http://localhost...?q=sdomain:ar,com,adsclasificados,* AND arc:"testWARCfiles/WIDE-201102...43.warc.gz"&rows=0&facet=true&facet.field=arc&facet.limit=-1](http://localhost:8983/solr/cdx/select?q=sdomain%3Aar%2Ccom%2Cadsclasificados%2C*+AND+arc%3A%22testWARCfiles/WIDE-20110225183219005-04371-13730~crawl301.us.archive.org~9443.warc.gz%22&sort=date+asc&wt=json&indent=true&fl=url)

The only tricky one it #8, which either requires two extra fields (which takes up space) or a potentially very heavy regexp. Or maybe a third solution is better: SURT the URL and split it into domain and path, not indexing the full URL at all? So instead of `query=url:"example.org/kittens.html` it would be `query=domain:"org.example" AND path="kittens.html"`. But that would make simple lookups more expensive in terms of processing power.

## Installation

Solr-cdx relies on Solr for all the heavy lifting. It is entirely possible to use a standalone Solr installation,
but this limits the maximum theoretical number of CDX entries to 2 billion. The provided scripts sets up a small
local SolrCloud, consisting of 2 Solrs and 3 ZooKeepers.

0. Adjust the setup (optional)
 a. Copy `scripts.default.conf` to `scripts.conf`  in the solr-cdx checkout-folder
 b. Copy `src/main/resources/solrcdx.default.properties` to `solrcdx.properties` in the solr-cdx checkout-folder
 c. Make any needed changes to the two new settings files
1. Build solr-cdx with `mvn package`
2. Install a local SolrCloud with `./install_cloud.sh`, start it with `./start_cloud.sh` and create a cdx-collection with `upload_and_link_config.sh`
3. Start the solr-cdx server with `java -jar target/solrcdx-*-SNAPSHOT-jar-with-dependencies.jar -server`


## Indexing

A working CDX sample can be downloaded from https://archive.org/details/testWARCfiles (the first one from 
"WARC CDX INDEX FILES" makes the sample links in this README work). The first line in the CDX file should be 
` CDX N b a m s k r M S V g` (check with `less file.cdx.gz | head -n 1`).

Index WARC files with `./index.sh *.cdx.gz`. This works with raw CDX files as well as GZipped CDX files.

## Testing
  
All scripts ends processing by outputting a specific URL to inspect.  

(yes, this section of the documentation should be expanded)

## See also

The [tinycdxserver](https://github.com/nla/tinycdxserver) looks very promising. Preliminary tests of solr-cdx indicates
that the storage needed to hold the index is fairly equal to the storage needed by the raw CDX-files. TinyCDX with
compression needs less than 1/5th of that space, so clear win to TinyCDX there.
 
Proper large-scale performance testing of solr-cdx vs. TinyCDX would be very relevant.
