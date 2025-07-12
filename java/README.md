### Interview Exercise

If companies want to keep making candidates invest time doing tech "homework" as part of the process they should also be made to invent new exercises now and again. This is a somewhat complete solution to the "homework" reqiured as part of the interview process more than six months ago, so here it is now pushed to my fork.

### Java

There is already a ```WebSystemCalls.java``` and corresponding implementation which allows you to get the HTTP headers for a URL, download the whole content or download the partial content. All these calls return an ```HttpResponse``` object, which contains sub properties for headers and the content.

As in the example here: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges, the HTTP header "Accept Ranges" will be set to "Bytes" if the CDN supports partial content.

A test project is included (with JUnit added though can be swapped for an alternative). There is also an App Main function which has a real Accurx URL (which **DOES** support partial content) and an example of a file path so that the code can be tested for real.
