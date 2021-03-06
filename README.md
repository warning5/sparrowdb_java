﻿[![Build Status](https://travis-ci.org/SparrowDb/sparrowdb.svg?branch=master)](https://travis-ci.org/SparrowDb/sparrowdb)
﻿
﻿Whats is SparrowDB?
====================
SparrowDB is an image database that works like an append-only object store. Sparrow has tools that allow image processing and HTTP server to access images.


Sparrow Object Store
====================
Sparrow consists of three files – the actual Sparrow store file containing the images data, plus an index file and a bloom filter file.

There is a corresponding data definition record followed by the image bytes for each image in the storage file. The index file provides the offset of the data definition in the storage file.


Requirements
====================
1. Java >= 1.8 (OpenJDK and Oracle JVMS have been tested)

Getting started
====================
This short guide will walk you through getting a basic one node cluster up and running, and demonstrate some simple reads and writes.

First, download Sparrow repository:

* Extract zip file
* Go to Sparrow directory
* Run: mvn clean install

After that we start the server. Running the startup script; it can be stopped with ctrl-C. Go to sparrowdb root directory.

	$ bin/sparrow.sh

Running client (default port 8082).

	$ bin/sparrow.sh client


Using Sparrow
====================
Creating a database:
	
	>>create database database_name;


Sending an image to database:

	>>insert into database_name ('image_path_with_extension', 'image_key');


Listing all images in database:

	>>select from database_name;
    key |    size| extension|           timestamp
    key1| 1184791|       png| 2016-04-20 00:51:35+0000
    key2|  104547|       jpg| 2016-04-20 01:04:07+0000
	key3|  558001|       png| 2016-01-03 22:14:31+0000
    key4|  558001|       gif| 2016-01-03 22:16:06+0000
    key5|	95889|       bmp| 2016-01-03 22:14:47+0000

    
You also can use query like:
	
	>>select from database_name where key = image_key;


Deleting image:

	>>delete from database_name where key = image_key;


Accessing image from browser:
	
	http://localhost:8081/database_name/image_key

License
====================
This software is under MIT license.
