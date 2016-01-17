# Data storage Configuration

[![Build Status](https://travis-ci.org/Bifroest/retentions.svg?branch=master)](https://travis-ci.org/Bifroest/retentions)

## Concepts

### Time, Days, Months and so on.
Time can be a highly complicated thing, especially if things need to be human readable.
This introduces time zones, summer time, hours occurring multiple times a day and 
things turn really, really weird really, really fast. However, in our storage, we don't 
have humans trying to understand what is going on. This greatly simplifies things, because 
we can introduce certain inaccuracies that simplify time handling. 

Most of our time handling derives from the following assumptions: 

 - Everything is measured in UTC-timestamps, that is, in seconds since
   1970-01-01T00:00:00 UTC.
 - The smallest, non-divisible unit of time is seconds in our data storage
 - Something like a day is defined as 86400 consecutive seconds. There might
   be leap seconds, or leap years, or other things like this, but a day
   is defined as all timestamps which, when divided by 86400, have the same
   remainder. 
   This will introduce a certain skew in time due to leap seconds, which grows
   over time, but that problem is fine with us.

The following date units are defined based on this:

| Time Unit | Definition | Config Notation |
| --------- | ---------- | --------------- |
| 1 second  | 1 second   | 1s, 5s, 60s     |
| 1 minute  | 60 seconds | 1m, 15m,        |
| 1 hour    | 60 minutes | 1h, 24h, 72h    |
| 1 day     | 24 hours   | 1d, 30d,        |
| 1 year    | 365 days   | 1y, 3y          |

There is no unit for month, because
 - m is used for minutes
 - A year (365 days) cannot be divided into months of equal length.

This table remains here for reference purposes only. Authorative definition is done by the 
enumeration com.goodgame.profiling.commons.statistics.units.TIME\_UNIT.

### Aggregation Functions
Our time-series data decreases in frequency in order to use storage space as efficiently as
possible. For example, we tend to keep 2 weeks with one datapoint per minute, and decrease
data density afterwards to something like 1 datapoint per hour for a few month. However, 
we need to maintain as much information as possible during this reduction. 

In order to do so, we **aggregate** our data points. In the previous example, once we have 
60 datapoints which are older than 2 weeks, we conceptually need to combine these high-density
datapoints into less-density datapoints. This is done using by applying the **aggregation function**
to the 60 old datapoints. The following aggregation functions are implemented at the moment:

**Average**. This is the *default* aggregation function and combines the values into the
average of the given values. No values are aggregated into a missing datapoint. This function
works well for most data, such as "number of users", "numbers of requests at this time" and so
on, which is why we use it as the default aggregation.

**Min** and **Max**: Min and Max are useful if the data fluctuates and outliers are interesting.
We have multiple examples for this in our current aggregation. For example, we aggregate
load using the max-aggregation in order to see high load or load spikes even after several
aggregations. On the other hand, we have entropy or the change between users 5 minutes ago
compared to users now. These are aggregated with a minimum, so we can see large user-drops
(i.e. user-changes far smaller than 100%) through multiple aggregations.

**Last**: This function is useful for monotonically increasing counters, such as the number 
of bytes transmitted by a network interface. In these cases, max or last aggregations have
a strong advantage compared to the average: They maintain the correct derivation across the
whole interval. For example, if we have an aggregated value of 5, and values \[10, 12, 16\] 
\[17, 18, 20\], in two adjacent aggregations, averaging will result in the value sequence 
5, 12.6 and 18.3. This means, the difference between the first and the last value is 13 now. 
On the other hand, if we use the max or last aggregation, we aggregate this into 5, 16, 20 
and maintain a correct difference of 15 from the first value to the last value.  We just 
lose accuracy inside the invervals, such as the relatively sharp increase from 12 to 16.

An observant reader will question why last exists, since last and max do the right thing.
Last is faster than max, since we don't need to look at all values to compute the last 
value.

## Configuration Syntax

Retention Levels and Aggregation Functions are implemented in the same mapping. This mapping
is a big json array of individual mappings. 

Each mapping must define the key "pattern". Pattern is a Java Regular Expression. If our
system is looking for a retention level to write to, or an aggregation function, we traverse
this list top-to-bottom. Once the pattern regex matches, we use whatever level or aggregation
is defined there.

For example, our current retention level configuration looks as follows:

```
"patterns": [
  {
    "pattern": ".*\\.Bifroest\\.(Bifroest|Aggregator|StreamRewriter)\\..*",
    "strategy": "many"
  }
]
```

The first pattern allows us to catch our internal data push, which pushes data at 1 datapoint per 10
seconds. The second pattern handles data from one of our teams, which generates data at one
datapoint per minute, and finally, we handle the entire rest with a strategy that contains
1 datapoint per 5 minutes.

The aggregation functions are defined in a separate file as such:

```
[
  {
    "pattern": "\\.System\\.CPU\\.(Interrupts|ContextSwitches)$",
    "method": "last"
  },
  {
    "pattern": "\\.System\\.CPU\\.IrqStats\\.[^.]*$",
    "method": "last"
  }
]
```

These follow a very straightforward pattern, they contain a pretty 
conservative pattern about the metrics they match, and apply the aggregation
method as necessary.

**Attention**: This part of the code still holds a subtle bug. For each file, we guarantee 
to traverse the pattern list  from top to bottom. However, right now we don't guarantee 
order if multiple files are merged. Thus, great care must be taken when defining overlapping
patterns or when defining catch-all retention levels.
