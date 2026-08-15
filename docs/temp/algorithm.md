## shortening algorithm 

### URL encoding 

#### base 62

base 62 are [0–9][a-z][A-Z]

How to convert a long URL to only 7 characters in base62 ? 

Technique 1: random string base62 -> check whether this string is in db or not. 

Technique 2: db generate a number -> convert into base62 

#### MD5

The MD5 message-digest algorithm is a widely used hash function producing a 128-bit hash value

Hash the long URL -> take only 7 characters for short URL -> if it existed -> try next 7 characters


### Key generation service 

Use a standalone KEY GENERATION SERVICE that generates random seven letter strings beforehand and stores them in a db. 
