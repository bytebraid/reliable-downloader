# Questions

## Updated Fri Jan 24 04:26:13 UTC 2025

## Addendum

My previous submission was rushed in time to arrive
before my Thursday 23rd interview, but since we didn't discuss any of the exercise I sought after investing a few extra hours to fill out the original omissions.

I hope you will disregard my first submission and make your assessments on this update.

New content:

 - JUnit tests
 - Refactored implementation
 - Additional features (FileProgress) and improvements
 - javadocs:java/app/build/docs/javadoc/com/accurx/reliabledownloader/package-summary.html
 - coverage:java/app/build/reports/jacoco/test/html/index.html
 - tests:java/app/build/reports/tests/test/classes/com.accurx.reliabledownloader.MyFileDownloadTest.html


## How did you approach solving the problem?

Implemented the checks, range, Content-MD5 and Content-Length, 
then proceeded with the async code to execute the downloads,
verify the content hash and rename the partial file.

Much more could be improved and added to for edge conditions.

The code requires some modest refactoring to improve readability
/ resuability.

Included some basic signal handling to interrupt the download
process as required.

## How did you verify your solution works correctly?

I underestimated the amount of time the task would require, so
only manual testing has been conducted.


## How long did you spend on the exercise?

2-3 hours


## What would you add if you had more time and how?

See the TODO statements in the code, use grep -C 3 -rie TODO  