package headline_crawler

import (
	"context"
	"service_articles/internal/model"
	"strings"
	"testing"
	"time"
	"tools"

	"github.com/microcosm-cc/bluemonday"
	"github.com/stretchr/testify/assert"
)

var testRss1 = `<?xml version="1.0" encoding="utf-8"?>
<rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0">
  <channel>
    <title>The Guardian</title>
    <link>https://www.theguardian.com/uk</link>
    <description>Latest news, sport, business, comment, analysis and reviews from the Guardian, the world's leading liberal voice</description>
    <language>en-gb</language>
    <copyright>Guardian News and Media Limited or its affiliated companies. All rights reserved. 2025</copyright>
    <pubDate>Sat, 14 Mar 2025 16:52:09 GMT</pubDate>
    <dc:date>2025-03-01T17:12:56Z</dc:date>
    <dc:language>en-gb</dc:language>
    <dc:rights>Guardian News and Media Limited or its affiliated companies. All rights reserved. 2025</dc:rights>
    <image>
      <title>The Guardian</title>
      <url>https://assets.guim.co.uk/images/guardian-logo-rss.c45beb1bafa34b347ac333af2e6fe23f.png</url>
      <link>https://www.theguardian.com</link>
    </image>
    <item>
      <title>Zelenskyy says ‘crucial’ for Ukraine to have Trump’s support in lengthy statement following Oval Office argument – live</title>
      <link>https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates</link>
      <description>&lt;p&gt;Ukrainian president has landed in the UK and will hold talks with PM Keir Starmer later&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/world/2025/mar/01/ukraine-reacts-zelenskyy-clash-trump"&gt;‘He defended our honour’: Ukrainians react to Zelenskyy’s clash with Trump&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Ukraine has destroyed 103 drones launched by Russia during an overnight strike, its air force has said.&lt;/p&gt;&lt;p&gt;In full: Zelenskyy and Trump meeting descends into heated argument in front of the press – video&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <pubDate>Sat, 14 Mar 2025 16:52:09 GMT</pubDate>
      <guid>https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/762c41c53d0bef46ae2f32744fbe359f74be911e/0_206_7451_4473/master/7451.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=55082309b73a0b76e0c6bb09d8cdf42a">
        <media:credit scheme="urn:ebu">Photograph: ABACA/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/762c41c53d0bef46ae2f32744fbe359f74be911e/0_206_7451_4473/master/7451.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4c42e2f36105e8ce2d593ffe21a7f670">
        <media:credit scheme="urn:ebu">Photograph: ABACA/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Nadeem Badshah (now); Daniel Lavelle, Hayden Vernon and Adam Fulton (earlier)</dc:creator>
      <dc:date>2025-03-01T16:52:09Z</dc:date>
    </item>
  </channel>
</rss>`

var testRss2 = `This XML file does not appear to have any style information associated with it. The document tree is shown below.
<rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:content="http://purl.org/rss/1.0/modules/content/" version="2.0">
<channel>
<title>NBC News Top Stories</title>
<link>https://www.nbcnews.com/</link>
<description>NBC News Top Stories</description>
<language>en-US</language>
<copyright>© 2025 NBCNews.com</copyright>
<lastBuildDate>Fri, 28 Feb 2025 17:57:12 GMT</lastBuildDate>
<image>
<url>https://media-cldnry.s-nbcnews.com/image/upload/MSNBC/Components/Media/NBC-promo-graphics/nbc-news.png</url>
<title>NBC News Top Stories</title>
<link>https://www.nbcnews.com/</link>
</image>
<item>
<guid isPermaLink="false">nbc_spec_trump_vance_zelenskyy_tense_250228</guid>
<title>Trump tells Zelenskyy he's 'gambling with World War III' in tense exchange</title>
<dateTimeWritten>Fri, 28 Feb 2025 20:33:10 GMT</dateTimeWritten>
<pubDate>Fri, 28 Feb 2025 17:57:12 GMT</pubDate>
<updateDate>Fri, 28 Feb 2025 20:33:10 GMT</updateDate>
<expires>2055-02-28T17:57:17.000+0000</expires>
<link>https://www.nbcnews.com/video/watch-full-video-trump-zelenskyy-and-vance-meeting-ends-in-heated-argument-233195589679</link>
<description>President Trump, Ukrainian President Zelenskyy and Vice President Vance had a tense exchange about the ongoing war with Russia. Vance asked Zelenskyy if he has ever said "thank you" for U.S. aid and Trump clashed with him over what has been provided.</description>
<media:content url="https://prodamdnewsencoding.akamaized.net/NBC_News_Digital/nbc_spec_trump_vance_zelenskyy_tense_250228/1/abs/index.m3u8" medium="video">
...
</media:content>
<media:thumbnail url="https://media-cldnry.s-nbcnews.com/image/upload/t_fit_1500w/mpx/2704722219/2025_02/1740765423580_nbc_spec_trump_vance_zelenskyy_tense_250228_1920x1080-njgfva.jpg"/>
</item>
</channel>
</rss>`

var testRss3 = `<rss version="2.0"><channel><title>Hacker News</title><link>https://news.ycombinator.com/</link><description>Links for the intellectually curious, ranked by readers.</description><item><title>NIH.gov DNS servers down, making PubMed, BLAST, etc. unreachable</title><link>https://www.nslookup.io/domains/www.nih.gov/dns-records/#authoritative</link><pubDate>Sun, 02 Mar 2025 10:50:52 +0000</pubDate><comments>https://news.ycombinator.com/item?id=43229201</comments><description><![CDATA[<a href="https://news.ycombinator.com/item?id=43229201">Comments</a>]]></description></item></channel></rss>`

func unwrapTime(t time.Time, e error) *time.Time {
	if e != nil {
		panic(e)
	}
	return &t
}

func TestRssParser1(t *testing.T) {
	expected := model.Headline{
		Title:       "Zelenskyy says ‘crucial’ for Ukraine to have Trump’s support in lengthy statement following Oval Office argument – live",
		Description: "Ukrainian president has landed in the UK and will hold talks with PM Keir Starmer later<a href=\"https://www.theguardian.com/world/2025/mar/01/ukraine-reacts-zelenskyy-clash-trump\">‘He defended our honour’: Ukrainians react to Zelenskyy’s clash with Trump</a>Ukraine has destroyed 103 drones launched by Russia during an overnight strike, its air force has said.In full: Zelenskyy and Trump meeting descends into heated argument in front of the press – video <a href=\"https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates\">Continue reading...</a>",
		Link:        "https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates",
		Date:        *unwrapTime(time.Parse(time.RFC1123, "Sat, 14 Mar 2025 16:52:09 GMT")),
		Creator:     tools.Ptr("Nadeem Badshah (now); Daniel Lavelle, Hayden Vernon and Adam Fulton (earlier)"),
	}

	reader := strings.NewReader(testRss1)
	policy := bluemonday.StrictPolicy()
	policy.AllowAttrs("href").OnElements("a")
	p := NewRssParser(policy)
	headlines, err := p.Parse(context.Background(), reader)

	assert.NoError(t, err)
	assert.Equal(t, expected, headlines[0])
}

func TestRssParser2(t *testing.T) {
	expected := model.Headline{
		Title:       "Trump tells Zelenskyy he's 'gambling with World War III' in tense exchange",
		Description: "President Trump, Ukrainian President Zelenskyy and Vice President Vance had a tense exchange about the ongoing war with Russia. Vance asked Zelenskyy if he has ever said \"thank you\" for U.S. aid and Trump clashed with him over what has been provided.",
		Link:        "https://www.nbcnews.com/video/watch-full-video-trump-zelenskyy-and-vance-meeting-ends-in-heated-argument-233195589679",
		Date:        *unwrapTime(time.Parse(time.RFC1123, "Fri, 28 Feb 2025 20:33:10 GMT")),
		Creator:     nil,
	}

	reader := strings.NewReader(testRss2)

	policy := bluemonday.StrictPolicy()
	policy.AllowAttrs("href").OnElements("a")
	p := NewRssParser(policy)
	headlines, e := p.Parse(context.Background(), reader)

	assert.NoError(t, e)
	assert.Equal(t, expected, headlines[0])
}

func TestRssParser3(t *testing.T) {
	expected := model.Headline{
		Title:       "NIH.gov DNS servers down, making PubMed, BLAST, etc. unreachable",
		Description: "<a href=\"https://news.ycombinator.com/item?id=43229201\">Comments</a>",
		Link:        "https://www.nslookup.io/domains/www.nih.gov/dns-records/#authoritative",
		Date:        *unwrapTime(time.Parse(time.RFC1123, "Sun, 02 Mar 2025 10:50:52 +0000")),
		Creator:     nil,
	}

	reader := strings.NewReader(testRss3)

	policy := bluemonday.StrictPolicy()
	policy.AllowAttrs("href").OnElements("a")
	p := NewRssParser(policy)
	headlines, e := p.Parse(context.Background(), reader)

	assert.NoError(t, e)
	assert.Equal(t, expected, headlines[0])
}
