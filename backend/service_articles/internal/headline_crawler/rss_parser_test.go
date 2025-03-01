package headline_crawler

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

var testRss = `<?xml version="1.0" encoding="utf-8"?>
<rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0">
  <channel>
    <title>The Guardian</title>
    <link>https://www.theguardian.com/uk</link>
    <description>Latest news, sport, business, comment, analysis and reviews from the Guardian, the world's leading liberal voice</description>
    <language>en-gb</language>
    <copyright>Guardian News and Media Limited or its affiliated companies. All rights reserved. 2025</copyright>
    <pubDate>Sat, 01 Mar 2025 17:12:56 GMT</pubDate>
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
      <pubDate>Sat, 01 Mar 2025 16:52:09 GMT</pubDate>
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
    <item>
      <title>‘He defended our honour’: Ukraine reacts to Zelenskyy’s clash with Trump</title>
      <link>https://www.theguardian.com/world/2025/mar/01/ukraine-reacts-zelenskyy-clash-trump</link>
      <description>&lt;p&gt;Back home there was widespread support for Ukraine’s president, but also dismay at his car-crash encounter in the Oval Office&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;strong&gt;&lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Live reaction to Zelenskyy’s clash with Trump and Vance&lt;/a&gt;&lt;/strong&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Ukrainians have rallied behind Volodymyr Zelenskyy after his mauling on Friday in the White House, and have accused Donald Trump and the US vice-president, JD Vance, of deliberately and cynically “starting a brawl”.&lt;/p&gt;&lt;p&gt;There was widespread support for Ukraine’s president at home and dismay at his car-crash encounter in the Oval Office. There was also praise for Zelenskyy’s insistence that a peace deal without security guarantees was meaningless, and that Russia could not be trusted.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/ukraine-reacts-zelenskyy-clash-trump"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <pubDate>Sat, 01 Mar 2025 12:52:22 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/ukraine-reacts-zelenskyy-clash-trump</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/169d23f05c8c43ff0c1920cbc3c5c8ac1e8910f4/0_298_6720_4032/master/6720.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9d6415ff377cd8844c9776285b36df53">
        <media:credit scheme="urn:ebu">Photograph: Alessio Mamo/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/169d23f05c8c43ff0c1920cbc3c5c8ac1e8910f4/0_298_6720_4032/master/6720.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ee37f1bcd3ddbd59254ee7deefa2d69e">
        <media:credit scheme="urn:ebu">Photograph: Alessio Mamo/The Observer</media:credit>
      </media:content>
      <dc:creator>Luke Harding in Kyiv</dc:creator>
      <dc:date>2025-03-01T12:52:22Z</dc:date>
    </item>
    <item>
      <title>‘A bigger victory for Putin than any military battle’: Russia gleeful after Trump-Zelenskyy clash</title>
      <link>https://www.theguardian.com/world/2025/mar/01/russia-trump-zelenskyy-ukraine-leader-oval-office-putin</link>
      <description>&lt;p&gt;Putin stays silent but Russian politicians and media outlets crow with delight  after the ambush of Ukraine’s leader&lt;/p&gt;&lt;p&gt;Russian officials and Moscow’s media outlets reacted with predictable glee to the &lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates?filterKeyEvents=false"&gt;dramatic clash between Volodymyr Zelenskyy and Donald Trump&lt;/a&gt; at the White House on Friday.&lt;/p&gt;&lt;p&gt;Posting on social media, &lt;a href="https://www.theguardian.com/world/dmitry-medvedev"&gt;Dmitry Medvedev&lt;/a&gt;, Putin’s deputy on the security council and former president, called the exchange “a brutal dressing-down in the Oval Office”.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/russia-trump-zelenskyy-ukraine-leader-oval-office-putin"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <category domain="https://www.theguardian.com/world/vladimir-putin">Vladimir Putin</category>
      <category domain="https://www.theguardian.com/world/dmitry-medvedev">Dmitry Medvedev</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <pubDate>Sat, 01 Mar 2025 15:48:23 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/russia-trump-zelenskyy-ukraine-leader-oval-office-putin</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/75b24ea1b6238eae13be2cbe8d392422dfc8d0c9/0_550_8256_4954/master/8256.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=938faf06e07c1877050c4e2b60e8ef34">
        <media:credit scheme="urn:ebu">Photograph: Saul Loeb/AFP/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/75b24ea1b6238eae13be2cbe8d392422dfc8d0c9/0_550_8256_4954/master/8256.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0536a485414c6691da8d81d197f7f9ba">
        <media:credit scheme="urn:ebu">Photograph: Saul Loeb/AFP/Getty Images</media:credit>
      </media:content>
      <dc:creator>Pjotr Sauer</dc:creator>
      <dc:date>2025-03-01T15:48:23Z</dc:date>
    </item>
    <item>
      <title>How JD Vance emerged as the chief saboteur of the transatlantic alliance</title>
      <link>https://www.theguardian.com/us-news/2025/feb/28/jd-vance-volodymyr-zelenskyy</link>
      <description>&lt;p&gt;Vance snaked his way in first to the row between Trump and Zelenskyy, his second intrusion this month after Munich&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Live reaction to Zelenskyy’s clash with Trump and Vance&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/us-news/jd-vance"&gt;JD Vance&lt;/a&gt; was supposed to be the inconsequential vice-president.&lt;/p&gt;&lt;p&gt;But his starring role in Friday’s &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-meeting-ukraine-aid-war"&gt;blowup between Donald Trump and Volodymyr Zelenskyy&lt;/a&gt; – where he played a cross between Trump’s bulldog and tech bro Iago – may mark the moment that the postwar alliance between Europe and America finally collapsed.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/jd-vance-volodymyr-zelenskyy"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <pubDate>Sat, 01 Mar 2025 07:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/feb/28/jd-vance-volodymyr-zelenskyy</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/9e2fbc08a9d0952c764c4840662ba5a8eb85d199/686_784_7506_4506/master/7506.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2b603278923268ccb40f852e998aca35">
        <media:credit scheme="urn:ebu">Photograph: Jim Lo Scalzo/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/9e2fbc08a9d0952c764c4840662ba5a8eb85d199/686_784_7506_4506/master/7506.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7ca331be4d22c64e8f5022f5b5c2f498">
        <media:credit scheme="urn:ebu">Photograph: Jim Lo Scalzo/EPA</media:credit>
      </media:content>
      <dc:creator>Andrew Roth in Washington</dc:creator>
      <dc:date>2025-03-01T07:00:35Z</dc:date>
    </item>
    <item>
      <title>Gunshots and a surge of panic: footage shows last moments of boy, 12, killed in the West Bank</title>
      <link>https://www.theguardian.com/world/2025/mar/01/footage-shows-last-moments-boy-12-killed-in-west-bank</link>
      <description>&lt;p&gt;Two children a week are killed in the West Bank. Two cameras recorded the circumstances of one such death&lt;/p&gt;&lt;p&gt;The last time Nassar al-Hammouni talked to his son, Ayman, it was by telephone and the 12-year-old was overflowing with plans for the coming weekend, and for the rest of his life. He had joined a local football team and planned to register at a karate club that weekend. When he grew up, he told Nassar, he was going to become a doctor, or better still an engineer to help his father in the construction job that took him away from their home in Hebron every week.&lt;/p&gt;&lt;p&gt;None of that – the football, the karate or his imagined future career – will happen now. Last Friday, two days after the call to his father, Ayman was killed, shot by Israeli fire, video footage seen by the Guardian suggests.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/footage-shows-last-moments-boy-12-killed-in-west-bank"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/west-bank">West Bank</category>
      <category domain="https://www.theguardian.com/world/palestinian-territories">Palestinian territories</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/world/middleeast">Middle East and north Africa</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/law/human-rights">Human rights</category>
      <pubDate>Sat, 01 Mar 2025 05:00:29 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/footage-shows-last-moments-boy-12-killed-in-west-bank</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/be4cd3e63a8d4a045624574ea5a7198477afabff/0_82_665_399/master/665.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=bcf395f4496782a22e1075f8e4d9e90a">
        <media:credit scheme="urn:ebu">Photograph: Family/Defense for Children International - Palestine</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/be4cd3e63a8d4a045624574ea5a7198477afabff/0_82_665_399/master/665.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ab1878d3549f951a2442da914ad8797a">
        <media:credit scheme="urn:ebu">Photograph: Family/Defense for Children International - Palestine</media:credit>
      </media:content>
      <dc:creator>Julian Borger and Sufian Taha in Hebron and Harrison Taylor</dc:creator>
      <dc:date>2025-03-01T05:00:29Z</dc:date>
    </item>
    <item>
      <title>PKK declares ceasefire with Turkey after more than 40 years of conflict</title>
      <link>https://www.theguardian.com/world/2025/mar/01/pkk-declares-ceasefire-with-turkey-after-40-years-kurdish</link>
      <description>&lt;p&gt;Kurdish militant group responds to call from its jailed leader, Abdullah Öcalan, to lay down arms&lt;/p&gt;&lt;p&gt;A Kurdish militia has declared a ceasefire in its 40-year insurgency against Turkey after its imprisoned leader called for the group to disarm and dissolve earlier this week.&lt;/p&gt;&lt;p&gt;“We are declaring a ceasefire to be effective from today on. None of our forces will take armed action unless attacked,” the executive committee of the Kurdistan Workers’ party (PKK) said in a statement.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/pkk-declares-ceasefire-with-turkey-after-40-years-kurdish"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/turkey">Turkey</category>
      <category domain="https://www.theguardian.com/world/kurds">Kurds</category>
      <category domain="https://www.theguardian.com/world/middleeast">Middle East and north Africa</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Sat, 01 Mar 2025 14:22:55 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/pkk-declares-ceasefire-with-turkey-after-40-years-kurdish</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/70f000e75afadb357c0a74d81dc82403d277234f/0_182_5472_3283/master/5472.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=39c7c193349fa5ec36e1abc722802f79">
        <media:credit scheme="urn:ebu">Photograph: Mehmet Masum Suer/SOPA Images/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/70f000e75afadb357c0a74d81dc82403d277234f/0_182_5472_3283/master/5472.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7fd348cc6d762bd005af1a348507a45a">
        <media:credit scheme="urn:ebu">Photograph: Mehmet Masum Suer/SOPA Images/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Ruth Michaelson and Faisal Ali</dc:creator>
      <dc:date>2025-03-01T14:22:55Z</dc:date>
    </item>
    <item>
      <title>Three teenage girls arrested over death of man, 75, in north London</title>
      <link>https://www.theguardian.com/uk-news/2025/mar/01/three-teenage-girls-arrested-over-death-of-man-75-in-north-london</link>
      <description>&lt;p&gt;Police launch a murder investigation after officers were called to an incident in Holloway on Thursday night&lt;/p&gt;&lt;p&gt;Three teenage girls have been arrested over the death of a 75-year-old man in north London.&lt;/p&gt;&lt;p&gt;Police launched a murder investigation after officers were called to an incident on Seven Sisters Road at 11.35pm on Thursday.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/mar/01/three-teenage-girls-arrested-over-death-of-man-75-in-north-london"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Sat, 01 Mar 2025 14:19:52 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/mar/01/three-teenage-girls-arrested-over-death-of-man-75-in-north-london</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/78c8b4dc195b1a3c4f0c13ad5680fd08fde3cd77/0_401_8256_4954/master/8256.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b446315ea4a0f3f7c28dad0f2d82c67d">
        <media:credit scheme="urn:ebu">Photograph: Jill Mead/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/78c8b4dc195b1a3c4f0c13ad5680fd08fde3cd77/0_401_8256_4954/master/8256.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=1855e8ac0a94ec2db4f35a67a770d7b5">
        <media:credit scheme="urn:ebu">Photograph: Jill Mead/The Guardian</media:credit>
      </media:content>
      <dc:creator>Hayden Vernon</dc:creator>
      <dc:date>2025-03-01T14:19:52Z</dc:date>
    </item>
    <item>
      <title>TikTok ‘craze’ behind Peak District bad parking crisis</title>
      <link>https://www.theguardian.com/environment/2025/mar/01/tiktok-craze-behind-peak-district-bad-parking-crisis-mam-tor</link>
      <description>&lt;p&gt;Local MP writes to authorities over ‘irresponsible’ motorists flocking to see sunrise and sunset at Mam tor&lt;/p&gt;&lt;p&gt;An MP has called for action on irresponsible parking at Peak District beauty spots that he says is being fuelled by a TikTok craze.&lt;/p&gt;&lt;p&gt;Jon Pearce, the Labour MP for High Peak in Derbyshire, said people had been flocking to the area to see the sunrise and sunset at Mam tor.&lt;/p&gt; &lt;a href="https://www.theguardian.com/environment/2025/mar/01/tiktok-craze-behind-peak-district-bad-parking-crisis-mam-tor"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/environment/national-parks">National parks</category>
      <category domain="https://www.theguardian.com/uk/transport">Transport</category>
      <category domain="https://www.theguardian.com/uk-news/derbyshire">Derbyshire</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/technology/tiktok">TikTok</category>
      <category domain="https://www.theguardian.com/politics/localgovernment">Local politics</category>
      <pubDate>Sat, 01 Mar 2025 11:43:41 GMT</pubDate>
      <guid>https://www.theguardian.com/environment/2025/mar/01/tiktok-craze-behind-peak-district-bad-parking-crisis-mam-tor</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/20af0118d6a857132e85149a2b475d6040f97f26/2_0_913_548/master/913.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ead4173d1ab588e32258583fa9536200">
        <media:credit scheme="urn:ebu">Photograph: Derbyshire County Council</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/20af0118d6a857132e85149a2b475d6040f97f26/2_0_913_548/master/913.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=5a7e37951f060620a75e32bd73095a4c">
        <media:credit scheme="urn:ebu">Photograph: Derbyshire County Council</media:credit>
      </media:content>
      <dc:creator>Hayden Vernon</dc:creator>
      <dc:date>2025-03-01T11:43:41Z</dc:date>
    </item>
    <item>
      <title>Residents trapped with service charges of up to £8,000 a year to take legal action against government</title>
      <link>https://www.theguardian.com/society/2025/mar/01/residents-trapped-with-service-charges-of-up-to-8000-a-year-to-take-legal-action-against-government</link>
      <description>&lt;p&gt;Owners of homes marketed by housing associations as ‘affordable’ are planning a challenge as service costs spiral&lt;/p&gt;&lt;p&gt;• &lt;a href="https://www.theguardian.com/money/2025/mar/01/theres-no-way-i-can-pay-london-residents-despair-of-steep-costs-and-forced-use-of-poor-door"&gt;‘There is no way I can pay’: residents despair of steep costs and ‘poor door’&lt;/a&gt;&lt;/p&gt;&lt;p&gt;Residents trapped in properties marketed as “affordable” are planning legal action against the government after being hit with service charges of up to £8,000 a year.&lt;/p&gt;&lt;p&gt;Shared-ownership homes are designed to allow people to get on the property ladder, with residents taking a mortgage on a share and paying subsidised rent on the rest. However, there are also service charges, which can initially be £250 to £350 a month. Once sold, some residents discover these charges can rise to £600 a month or more.&lt;/p&gt; &lt;a href="https://www.theguardian.com/society/2025/mar/01/residents-trapped-with-service-charges-of-up-to-8000-a-year-to-take-legal-action-against-government"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/society/housing">Housing</category>
      <category domain="https://www.theguardian.com/society/social-housing">Social housing</category>
      <category domain="https://www.theguardian.com/money/property">Property</category>
      <category domain="https://www.theguardian.com/money/money">Money</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 16:59:03 GMT</pubDate>
      <guid>https://www.theguardian.com/society/2025/mar/01/residents-trapped-with-service-charges-of-up-to-8000-a-year-to-take-legal-action-against-government</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/9e352b732f07ccb17262e3f474f362126c289ee9/0_0_5616_3370/master/5616.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=53a460eaf5e79bff59e40502fcaf1104">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/9e352b732f07ccb17262e3f474f362126c289ee9/0_0_5616_3370/master/5616.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=14dc7d8b5af0e2ea297c2aef13857343">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <dc:creator>Jon Ungoed-Thomas and Tom Wall</dc:creator>
      <dc:date>2025-03-01T16:59:03Z</dc:date>
    </item>
    <item>
      <title>Former Brookside actor sentenced to jail over sham modelling agencies</title>
      <link>https://www.theguardian.com/uk-news/2025/mar/01/former-brookside-actor-sentenced-to-jail-over-sham-modelling-agencies</link>
      <description>&lt;p&gt;Philip Foster lives in Spain and was sentenced in his absence after defrauding more than 6,000 people &lt;/p&gt;&lt;p&gt;A former TV soap actor has been sentenced to eight-and-a-half years in prison after masterminding a £13.6m fraud that targeted aspiring models.&lt;/p&gt;&lt;p&gt;Former Brookside actor Philip Foster, 49, ran an operation involving a network of sham modelling agencies for more than eight years.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/mar/01/former-brookside-actor-sentenced-to-jail-over-sham-modelling-agencies"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk/ukcrime">Crime</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/tv-and-radio/soap-opera">Soap opera</category>
      <category domain="https://www.theguardian.com/culture/television">Television</category>
      <pubDate>Sat, 01 Mar 2025 16:56:07 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/mar/01/former-brookside-actor-sentenced-to-jail-over-sham-modelling-agencies</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/433514853482a1ffc1cbe7e0e99eeb64258d092e/0_444_1186_711/master/1186.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0514e8fad6aeafb214ad8b337fd4eb6b">
        <media:credit scheme="urn:ebu">Photograph: NTS/PA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/433514853482a1ffc1cbe7e0e99eeb64258d092e/0_444_1186_711/master/1186.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=23b92b3c50afd63f2266385d5eca2b1a">
        <media:credit scheme="urn:ebu">Photograph: NTS/PA</media:credit>
      </media:content>
      <dc:creator>Hayden Vernon</dc:creator>
      <dc:date>2025-03-01T16:56:07Z</dc:date>
    </item>
    <item>
      <title>Kennedy Jr backtracks and says US measles outbreak is now a ‘top priority’ for health department</title>
      <link>https://www.theguardian.com/us-news/2025/mar/01/kennedy-jr-measles-outbreak-health-department</link>
      <description>&lt;p&gt;Health secretary earlier said outbreak was ‘not unusual’ but with first US measles death in decade steps up response&lt;/p&gt;&lt;p&gt;Two days after initially downplaying the outbreak as “not unusual,” the US health secretary, &lt;a href="https://www.theguardian.com/us-news/robert-f-kennedy-jr"&gt;Robert F Kennedy&lt;/a&gt; Jr, on Friday said he recognizes the serious impact of the ongoing measles epidemic in Texas – in which a child died recently – and said the government is providing resources, including protective vaccines.&lt;/p&gt;&lt;p&gt;“Ending the measles outbreak is a top priority for me and my extraordinary team,” Kennedy – an avowed anti-vaccine conspiracy theorist who for years has sown doubts about the safety and efficacy of vaccines – said in a post on X.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/mar/01/kennedy-jr-measles-outbreak-health-department"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/robert-f-kennedy-jr">Robert F Kennedy Jr</category>
      <category domain="https://www.theguardian.com/society/mmr">MMR</category>
      <category domain="https://www.theguardian.com/science/infectiousdiseases">Infectious diseases</category>
      <category domain="https://www.theguardian.com/us-news/healthcare">US healthcare</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/texas">Texas</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <pubDate>Sat, 01 Mar 2025 14:26:58 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/mar/01/kennedy-jr-measles-outbreak-health-department</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/8b486df5e905b64dd72b07822c7dbe492dcc64b7/0_428_5712_3428/master/5712.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d42f5638c737e8e88240dedf0eb9fc54">
        <media:credit scheme="urn:ebu">Photograph: Sebastian Rocandio/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/8b486df5e905b64dd72b07822c7dbe492dcc64b7/0_428_5712_3428/master/5712.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9f4197cf87bf2dac257ee6d550ef8b5d">
        <media:credit scheme="urn:ebu">Photograph: Sebastian Rocandio/Reuters</media:credit>
      </media:content>
      <dc:creator>Guardian staff and agencies</dc:creator>
      <dc:date>2025-03-01T14:26:58Z</dc:date>
    </item>
    <item>
      <title>Cash Isas: pressure grows against rumoured move to £4,000 allowance</title>
      <link>https://www.theguardian.com/money/2025/mar/01/cash-isas-4000-allowance-tax-free-accounts-limit</link>
      <description>&lt;p&gt;Research shows strong support for keeping tax-free accounts in their current form with £20,000 annual limit&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://theguardian.com/money/2025/feb/28/cash-isa-savers-flexible-rules-interest-rates-account"&gt;Cash Isa providers fail to offer savers benefits of more flexible rules&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;A campaign to “save” cash Isas gathered pace this week, with research published showing strong support for the savings accounts.&lt;/p&gt;&lt;p&gt;However, data was also issued that investment firms said showed UK savers were “paying the price” for playing it safe because putting money into the stock market can generate much higher returns.&lt;/p&gt; &lt;a href="https://www.theguardian.com/money/2025/mar/01/cash-isas-4000-allowance-tax-free-accounts-limit"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/money/isas">Isas</category>
      <category domain="https://www.theguardian.com/money/savings">Savings</category>
      <category domain="https://www.theguardian.com/money/banks">Banks and building societies</category>
      <category domain="https://www.theguardian.com/money/money">Money</category>
      <category domain="https://www.theguardian.com/money/consumer-affairs">Consumer affairs</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/money/investment-isas">Investment Isas</category>
      <category domain="https://www.theguardian.com/money/moneyinvestments">Investments</category>
      <category domain="https://www.theguardian.com/money/shares">Shares</category>
      <category domain="https://www.theguardian.com/money/investmentfunds">Investment funds</category>
      <pubDate>Sat, 01 Mar 2025 07:00:34 GMT</pubDate>
      <guid>https://www.theguardian.com/money/2025/mar/01/cash-isas-4000-allowance-tax-free-accounts-limit</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/2b07e73bc3b99a6df01428b9d1069454a94bdbbf/0_208_6240_3744/master/6240.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4e06a66ce6aa0a01d1eec102303bb5cd">
        <media:credit scheme="urn:ebu">Photograph: mundissima/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/2b07e73bc3b99a6df01428b9d1069454a94bdbbf/0_208_6240_3744/master/6240.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4df8dde50662f31c55e8c987266fe987">
        <media:credit scheme="urn:ebu">Photograph: mundissima/Alamy</media:credit>
      </media:content>
      <dc:creator>Rupert Jones</dc:creator>
      <dc:date>2025-03-01T07:00:34Z</dc:date>
    </item>
    <item>
      <title>‘The grapes won’t wait’: Lebanese winemakers fight to survive as war rages</title>
      <link>https://www.theguardian.com/world/2025/mar/01/lebanese-winemakers-war-bekaa-valley-israeli-strikes</link>
      <description>&lt;p&gt;Owners of vineyards in the Bekaa valley are focused more on Israeli air strikes than this year’s vintage. How are these family-run businesses coping?&lt;/p&gt;&lt;p&gt;In September Elias Maalouf and his father were sitting in &lt;a href="https://www.chateaurayak.com/"&gt;Chateau Rayak&lt;/a&gt;, the family winery in the Bekaa valley in Lebanon, when they decided to head home for a lunch break. Five minutes later an Israeli jet dropped a bomb on a house across the street, crushing the three-storey building and destroying much of the winery.&lt;/p&gt;&lt;p&gt;“If we hadn’t left we would have died,” said 41-year-old Maalouf, sitting in the winery as repair workers replaced a shattered television five months later. The doors had blown in from the force of the blast and shattered glass had rained down on the table where he now sat, the wood of the furniture still pockmarked from shrapnel.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/lebanese-winemakers-war-bekaa-valley-israeli-strikes"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/lebanon">Lebanon</category>
      <category domain="https://www.theguardian.com/world/hezbollah">Hezbollah</category>
      <category domain="https://www.theguardian.com/world/middleeast">Middle East and north Africa</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/israel-hamas-war">Israel-Gaza war</category>
      <category domain="https://www.theguardian.com/food/wine">Wine</category>
      <category domain="https://www.theguardian.com/society/alcohol">Alcohol</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 16:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/lebanese-winemakers-war-bekaa-valley-israeli-strikes</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/651fcf9d57bd124f1e36679b303ea32b83dfa420/0_267_4000_2400/master/4000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7d10fb4fa0ecdd13978538edcf8928ff">
        <media:credit scheme="urn:ebu">Photograph: Oliver Marsden</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/651fcf9d57bd124f1e36679b303ea32b83dfa420/0_267_4000_2400/master/4000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=341dece751be14eb68e23bd3bedd8735">
        <media:credit scheme="urn:ebu">Photograph: Oliver Marsden</media:credit>
      </media:content>
      <dc:creator>William Christou  Bekaa valley</dc:creator>
      <dc:date>2025-03-01T16:00:44Z</dc:date>
    </item>
    <item>
      <title>Sex 20 times a week? New study identifies four types of romantic lover</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/sex-20-times-a-week-new-study-identifies-four-types-of-romantic-lover</link>
      <description>&lt;p&gt;Australian research is ‘first to empirically show that we don’t all love the same’, lead author says&lt;/p&gt;&lt;ul&gt;&lt;li&gt;Get our &lt;a href="https://www.theguardian.com/email-newsletters?CMP=cvau_sfl"&gt;breaking news email&lt;/a&gt;, &lt;a href="https://app.adjust.com/w4u7jx3"&gt;free app&lt;/a&gt; or &lt;a href="https://www.theguardian.com/australia-news/series/full-story?CMP=cvau_sfl"&gt;daily news podcast&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;New research has identified four types of romantic lover, including one that has sex up to 20 times a week.&lt;/p&gt;&lt;p&gt;The research, published in the journal &lt;a href="https://www.sciencedirect.com/science/article/pii/S0191886925000704?via%3Dihub"&gt;Personality and Individual Differences&lt;/a&gt;, categorised lovers as mild romantic, moderate romantic, intense romantic, and libidinous romantic.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;&lt;a href="https://www.theguardian.com/email-newsletters?CMP=copyembed"&gt;Sign up for Guardian Australia’s breaking news email&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Mild: &lt;/strong&gt;About one in five – 20.02% – fell into this cluster, characterised by “the lowest intensity, lowest obsessive thinking, lowest commitment, and lowest frequency of sex”. This group also had the lowest proportion of people who thought their partner was “definitely” in love with them – just 25.31% – and the lowest proportion having sex, at 82.72%.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Moderate: &lt;/strong&gt;About four in 10 – 40.91% – landed in this category, which Bode described as “fairly stock-standard” – or in the words of the journal article, “entirely unremarkable”. Those in this category were more likely to be male, and less likely to have children. This group had “relatively low intensity, relatively low obsessive thinking, relatively high commitment, and relatively moderate frequency of sex”.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Intense: &lt;/strong&gt;This category described about one in three – 29.42% – of survey respondents, who Bode described as “crazy in-love” types. They were characterised by “the highest intensity, highest obsessive thinking, highest commitment, and relatively high frequency of sex”. About six in 10 people in this group were female.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Libidinous: &lt;/strong&gt;About one in 10 – 9.64% – were libidinous romantic lovers, who had sex an average of 10 times a week and up to 20 times. They were characterised as “relatively high intensity, relatively high obsessive thinking, relatively high commitment, and exceptionally high frequency of sex”. This group were slightly more likely to be male, and had the highest proportion of people in a committed relationship but not living together.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/sex-20-times-a-week-new-study-identifies-four-types-of-romantic-lover"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/relationships">Relationships</category>
      <category domain="https://www.theguardian.com/science/science">Science</category>
      <category domain="https://www.theguardian.com/science/psychology">Psychology</category>
      <category domain="https://www.theguardian.com/australia-news/australia-news">Australia news</category>
      <category domain="https://www.theguardian.com/science/biology">Biology</category>
      <category domain="https://www.theguardian.com/science/anthropology">Anthropology</category>
      <pubDate>Fri, 28 Feb 2025 23:00:22 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/sex-20-times-a-week-new-study-identifies-four-types-of-romantic-lover</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/20fb8153df8ca00ed2e36a7cdddffd51df435e85/0_88_6240_3744/master/6240.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=cf4f5d5ca251ed73021c1687e145b9dc">
        <media:credit scheme="urn:ebu">Photograph: Gerardo Vieyra/NurPhoto/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/20fb8153df8ca00ed2e36a7cdddffd51df435e85/0_88_6240_3744/master/6240.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=14189a93135fef2846c81fb3090dde26">
        <media:credit scheme="urn:ebu">Photograph: Gerardo Vieyra/NurPhoto/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Tory Shepherd</dc:creator>
      <dc:date>2025-02-28T23:00:22Z</dc:date>
    </item>
    <item>
      <title>‘There’s no way I can pay’: London residents despair of steep costs and forced use of ‘poor door’</title>
      <link>https://www.theguardian.com/money/2025/mar/01/theres-no-way-i-can-pay-london-residents-despair-of-steep-costs-and-forced-use-of-poor-door</link>
      <description>&lt;p&gt;‘Affordable housing’ home owners complain of paying high charges for facilities mainly used by better-off residents&lt;/p&gt;&lt;p&gt;•&lt;a href="https://www.theguardian.com/society/2025/mar/01/residents-trapped-with-service-charges-of-up-to-8000-a-year-to-take-legal-action-against-government"&gt; Residents trapped with service charges of up to £8,000 a year&lt;/a&gt;&lt;/p&gt;&lt;p&gt;Marco Scalvini was thrilled to move into a &lt;a href="https://www.theguardian.com/money/2022/sep/10/shared-ownership-cost-buy-home-property"&gt;shared ownership flat&lt;/a&gt; on a stylish development in south London. “I felt like I had won the lottery. The apartment was beautiful. It was central and near to the university [where I work]. The price was affordable … compared with the private market,” he says.&lt;/p&gt;&lt;p&gt;Scalvini, a lecturer, met the criteria for affordable housing: he was a first-time buyer and had been priced out of the &lt;a href="https://www.theguardian.com/money/2025/jan/28/london-house-sales-brexit-foxtons-profits"&gt;capital’s housing market&lt;/a&gt;. But in the past year, his dream has turned into a nightmare. Peabody – the housing association managing the affordable flats in the development – has increased his service charge by 77%: he has gone from paying about £4,500 in 2023/24 to about £8,000 in 2024/25.&lt;/p&gt; &lt;a href="https://www.theguardian.com/money/2025/mar/01/theres-no-way-i-can-pay-london-residents-despair-of-steep-costs-and-forced-use-of-poor-door"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/society/housing">Housing</category>
      <category domain="https://www.theguardian.com/inequality/inequality">Inequality</category>
      <category domain="https://www.theguardian.com/uk/london">London</category>
      <pubDate>Sat, 01 Mar 2025 16:59:08 GMT</pubDate>
      <guid>https://www.theguardian.com/money/2025/mar/01/theres-no-way-i-can-pay-london-residents-despair-of-steep-costs-and-forced-use-of-poor-door</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/513e72b641368b87ed9597903517c9253f7203b9/0_342_5433_3260/master/5433.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f4ac89262be24e5534919957fa4f2fe2">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/513e72b641368b87ed9597903517c9253f7203b9/0_342_5433_3260/master/5433.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7c71468b35556d3186516d05c2549326">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <dc:creator>Tom Wall and Jon Ungoed-Thomas</dc:creator>
      <dc:date>2025-03-01T16:59:08Z</dc:date>
    </item>
    <item>
      <title>How do we make Europe more secure? Here are five steps we need to take now</title>
      <link>https://www.theguardian.com/world/2025/mar/01/how-do-we-make-europe-more-secure-here-are-five-steps-we-need-to-take-now</link>
      <description>&lt;p&gt;Europe can’t wait to react to Trump’s mood swings but must show we have the will and the wallet to take back control&lt;/p&gt;&lt;p&gt;• &lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Ukraine war live&lt;/a&gt;&lt;/p&gt;&lt;p&gt;It’s exhausting and humiliating to have no control – watching every meeting in the Oval Office for a glimmer of Trump’s approval or displeasure, our security resting on a perceived slight or a mood.&lt;/p&gt;&lt;p&gt;The last week of meetings between Trump, Macron, Starmer and &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-meeting-ukraine-aid-war"&gt;finally Zelenskyy&lt;/a&gt; always felt like crawling across a minefield. Some might agonise about whether Zelenskyy could have played things differently. It’s the wrong question. The point is that we can’t carry on being so dependent on every meeting at the White House. Until we start taking charge of our future, we will always be one heart palpitation away from dreading doomsday.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/how-do-we-make-europe-more-secure-here-are-five-steps-we-need-to-take-now"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <category domain="https://www.theguardian.com/world/vladimir-putin">Vladimir Putin</category>
      <category domain="https://www.theguardian.com/politics/defence">Defence policy</category>
      <category domain="https://www.theguardian.com/uk/military">Military</category>
      <category domain="https://www.theguardian.com/world/arms-trade">Arms trade</category>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Sat, 01 Mar 2025 16:29:52 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/how-do-we-make-europe-more-secure-here-are-five-steps-we-need-to-take-now</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/6fd49d68fcd62ac5deadad0b8b10a29a10687172/0_185_5000_3001/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7f4eebbcdbe5924b221a34f3c82754d3">
        <media:credit scheme="urn:ebu">Photograph: Hannes P Albert/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/6fd49d68fcd62ac5deadad0b8b10a29a10687172/0_185_5000_3001/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d7a86f134169b2058ce25df65ec75e00">
        <media:credit scheme="urn:ebu">Photograph: Hannes P Albert/AP</media:credit>
      </media:content>
      <dc:creator>Peter Pomerantsev</dc:creator>
      <dc:date>2025-03-01T16:29:52Z</dc:date>
    </item>
    <item>
      <title>‘It’s packed with dealers. Look around you’: life amid the cocaine cartels of the French Riviera</title>
      <link>https://www.theguardian.com/world/2025/mar/01/cocaine-cartels-french-riviera-nice-drug-gangs</link>
      <description>&lt;p&gt;Nice may be at the heart of France’s upmarket Mediterranean coast, but violent drug gangs making €1.5m a month are colonising part of it&lt;/p&gt;&lt;p&gt;The sight of the gun tucked into the man’s trousers told us it was time to go. We had been in one of France’s most notorious estates for several hours, trying to understand life on the frontline of the country’s spiralling drug war.&lt;/p&gt;&lt;p&gt;Seeing three people he did not know and a camera, he decided enough was enough. “You, where do you live?” he said, rushing towards us from the foot of a tower block where he had parked his scooter. “Don’t talk back to me, I’ll break your head in. Get out of here.”&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/cocaine-cartels-french-riviera-nice-drug-gangs"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/france">France</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/society/drugs">Drugs</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 16:00:42 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/cocaine-cartels-french-riviera-nice-drug-gangs</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/0c33bad88669537aef385e88a0215456512d3825/0_74_4702_2822/master/4702.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2ed0587c4e246949ba3c181d8c7267ea">
        <media:credit scheme="urn:ebu">Photograph: Eric Gaillard/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/0c33bad88669537aef385e88a0215456512d3825/0_74_4702_2822/master/4702.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3b7674f1e88505ea13e5da9af55222ba">
        <media:credit scheme="urn:ebu">Photograph: Eric Gaillard/Reuters</media:credit>
      </media:content>
      <dc:creator>Richard Assheton in Nice</dc:creator>
      <dc:date>2025-03-01T16:00:42Z</dc:date>
    </item>
    <item>
      <title>The number of people with chronic conditions is soaring. Are we less healthy than we used to be – or overdiagnosing illness?</title>
      <link>https://www.theguardian.com/society/2025/mar/01/the-number-of-people-with-chronic-conditions-is-soaring-are-we-less-healthy-than-we-used-to-be-or-overdiagnosing-illness</link>
      <description>&lt;p&gt;Are ordinary life experiences, bodily imperfections and normal differences being unnecessarily pathologised? One doctor argues just that&lt;/p&gt;&lt;p&gt;School was a difficult time for Anna. It still haunts her. She recalls being a sociable child, good at making friends. But she also remembers becoming hyperfixated on one friend, then another and another in succession. She tended to be impulsive and, wanting to please others, easily led. One distressing incident in particular has never left her. On the first day after moving to a new school, she was relieved to be taken under the wing of two girls. At lunchtime, in fits of giggles, the girls egged each other on to do naughty things. Anna spat orange juice at the boys. She did it with relish, only to reproach herself later. She feels the episode coloured her whole school experience.&lt;/p&gt;&lt;p&gt;As a child and an adult, Anna felt sanctioned, judged and misunderstood. She considers herself a chameleon who adapts to new environments and survives by being funny, but all too often regrets things she has said. Her self-esteem is low. Anna is a nurse and, although she loves her job and is good at it, she still often feels inadequate. “People don’t think I’m as clever as I feel. I can’t get the words out quickly enough,” she says.&lt;/p&gt; &lt;a href="https://www.theguardian.com/society/2025/mar/01/the-number-of-people-with-chronic-conditions-is-soaring-are-we-less-healthy-than-we-used-to-be-or-overdiagnosing-illness"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/society/mental-health">Mental health</category>
      <category domain="https://www.theguardian.com/society/neurodiversity">Neurodiversity</category>
      <category domain="https://www.theguardian.com/society/attention-deficit-hyperactivity-disorder">Attention deficit hyperactivity disorder</category>
      <category domain="https://www.theguardian.com/society/long-covid">Long Covid</category>
      <category domain="https://www.theguardian.com/society/autism">Autism</category>
      <category domain="https://www.theguardian.com/society/chronic-fatigue-syndrome">ME / Chronic fatigue syndrome</category>
      <category domain="https://www.theguardian.com/science/medical-research">Medical research</category>
      <category domain="https://www.theguardian.com/science/science">Science</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 14:00:42 GMT</pubDate>
      <guid>https://www.theguardian.com/society/2025/mar/01/the-number-of-people-with-chronic-conditions-is-soaring-are-we-less-healthy-than-we-used-to-be-or-overdiagnosing-illness</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b92dfe43b2bf8848d335e37d4cd3e2ffc379a053/0_679_5188_3112/master/5188.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a392b783fd63dda06ca30cbcee977632">
        <media:credit scheme="urn:ebu">Illustration: Anna Parini/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b92dfe43b2bf8848d335e37d4cd3e2ffc379a053/0_679_5188_3112/master/5188.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=69aaf86b5ad2fbf5ef08b3e1d3145148">
        <media:credit scheme="urn:ebu">Illustration: Anna Parini/The Guardian</media:credit>
      </media:content>
      <dc:creator>Suzanne O'Sullivan</dc:creator>
      <dc:date>2025-03-01T14:00:42Z</dc:date>
    </item>
    <item>
      <title>Holidays at a one-time Communist luxury beach haven? Yugoslav resort built for Tito to rise from ruins</title>
      <link>https://www.theguardian.com/world/2025/mar/01/communist-tito-yugoslav-adriatic-luxury-beach-resort</link>
      <description>&lt;p&gt;Revolutionary leader’s derelict Adriatic getaway is on verge of a new life after starring in Kate Winslet film and hit Mr Beast YouTube video&lt;/p&gt;&lt;p&gt;With its spectacular views over the Adriatic and a half-moon beach, Kupari, near Dubrovnik, was regarded as the Monaco of what was once called the Yugoslav Riviera.&lt;/p&gt;&lt;p&gt;In the 1960s, the country’s Communist leader, Josip “Tito” Broz, ordered the building of a vast holiday complex exclusively reserved for members of the military on this hilly stretch of the Dalmatian coast. For the top brass there were individual villas; for the lower ranks, a choice of six hotels, while the foot soldiers were relegated to a camping site surrounded by palm trees and lush greenery.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/communist-tito-yugoslav-adriatic-luxury-beach-resort"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/croatia">Croatia</category>
      <category domain="https://www.theguardian.com/travel/travel">Travel</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/communism">Communism</category>
      <category domain="https://www.theguardian.com/travel/dubrovnik">Dubrovnik holidays</category>
      <category domain="https://www.theguardian.com/travel/croatia">Croatia holidays</category>
      <pubDate>Sat, 01 Mar 2025 15:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/communist-tito-yugoslav-adriatic-luxury-beach-resort</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5379ded8f8b80c844dcd89b2fe14d33442df3c3f/49_27_735_441/master/735.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c35c403b2cd0e8a0e8146677de00176c">
        <media:credit scheme="urn:ebu">Photograph: No Credit</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5379ded8f8b80c844dcd89b2fe14d33442df3c3f/49_27_735_441/master/735.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=020469e0bde7f8616397ce7d5f619017">
        <media:credit scheme="urn:ebu">Photograph: No Credit</media:credit>
      </media:content>
      <dc:creator>Kim Willsher in Kupari</dc:creator>
      <dc:date>2025-03-01T15:00:43Z</dc:date>
    </item>
    <item>
      <title>From Unforgiven to The Firm: Guardian writers pick their favourite Gene Hackman movies</title>
      <link>https://www.theguardian.com/film/2025/mar/01/from-unforgiven-to-the-firm-guardian-writers-pick-their-favourite-gene-hackman-movies</link>
      <description>&lt;p&gt;After the death of the esteemed actor, writers highlight their favourite movies from a long and varied career&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/film/2025/feb/28/gene-hackman-clint-eastwood-generation"&gt;‘He let us hate him’: Gene Hackman had a rare power – he didn’t need to be liked&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Almost five minutes go by in The French Connection before we get a good look at Gene Hackman. Various other operators come and go in William Friedkin’s gritty and unsettling procedural – based on a real heroin sting – before Hackman’s Detective Jimmy “Popeye” Doyle emerges from behind an ill-fitting undercover Santa Claus outfit, like a background player busting into his first lead role. It’s as fitting an entrance as ever for Hackman, leveling up after his supporting work on TV and films like Bonnie and Clyde. And he gives a performance that sets the tone for his whole career, playing the brutal and racist cop, a morally murky figure who just doesn’t sit right as the hero of the story. Many of the qualities that made Hackman so great in later villainous roles – the way he moves like a menace with a devilishly charming grin, slipping so easily from comforting to antagonizing – are in Doyle. That detective’s infamous query, repeatedly grilling suspects about picking their feet in Poughkeepsie, is as mischievously disorienting as Hackman’s onscreen presence. &lt;em&gt;Radheyan Simonpillai&lt;/em&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/film/2025/mar/01/from-unforgiven-to-the-firm-guardian-writers-pick-their-favourite-gene-hackman-movies"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/film/gene-hackman">Gene Hackman</category>
      <category domain="https://www.theguardian.com/film/the-conversation">The Conversation</category>
      <category domain="https://www.theguardian.com/film/clinteastwood">Clint Eastwood</category>
      <category domain="https://www.theguardian.com/film/francis-ford-coppola">Francis Ford  Coppola</category>
      <category domain="https://www.theguardian.com/film/film">Film</category>
      <category domain="https://www.theguardian.com/film/william-friedkin">William Friedkin</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <pubDate>Sat, 01 Mar 2025 15:06:10 GMT</pubDate>
      <guid>https://www.theguardian.com/film/2025/mar/01/from-unforgiven-to-the-firm-guardian-writers-pick-their-favourite-gene-hackman-movies</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b8b452debfcb2c5f3632dc7aa4b49e3171593939/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=cc63c9be808e5f0808e29e6c85430b84">
        <media:credit scheme="urn:ebu">Composite: Allstar/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b8b452debfcb2c5f3632dc7aa4b49e3171593939/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d8b8313c5d728d746c0cca3cb3dec5b8">
        <media:credit scheme="urn:ebu">Composite: Allstar/Alamy</media:credit>
      </media:content>
      <dc:creator>Radheyan Simonpillai, Andrew Pulver, Benjamin Lee, Charles Bramesco, Andrew Lawrence, Pamela Hutchinson, Scott Tobias and Jesse Hassenger</dc:creator>
      <dc:date>2025-03-01T15:06:10Z</dc:date>
    </item>
    <item>
      <title>No pain, all gain: how to get stronger and build more muscle</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/no-pain-all-gain-how-to-get-stronger-and-build-more-muscle</link>
      <description>&lt;p&gt;Hitting the gym will make you fitter and happier, but not all strength work is equal. Here are the smart choices to make – with weights and without – for all ages &lt;/p&gt;&lt;p&gt;Gym culture is changing. Once the preserve of musclemen whose veins looked ready to pop, now muscle-strengthening activities are being advised by the NHS &lt;a href="https://www.nhs.uk/live-well/exercise/physical-activity-guidelines-for-adults-aged-19-to-64/"&gt;for those aged 19-64&lt;/a&gt; at least twice a week. That’s because an increasing body of evidence links strength work with wellbeing and longevity, including a &lt;a href="https://www.mdpi.com/2079-7737/13/11/883"&gt;2024 study&lt;/a&gt; that showed 90 minutes of strength training a week resulted in four years less biological ageing. Maybe that’s why about 15% of the UK population is now a member of a gym. Part of the appeal is accessibility – it’s not as technical as swimming, for example – but despite its simplicity, there’s a huge amount of misinformation and conflicting advice.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Low or high reps?&lt;/strong&gt;&lt;br&gt;
 Strength-training exercises are structured into a number of sets made up of repetitions. For instance, eight lifts, rest, followed by eight further lifts equates to two sets of eight. Finding the optimal combination of sets, reps and rest for gaining strength is a well-worn gym debate, but science is beginning to settle on an answer.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/no-pain-all-gain-how-to-get-stronger-and-build-more-muscle"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/fitness">Fitness</category>
      <category domain="https://www.theguardian.com/science/science">Science</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <category domain="https://www.theguardian.com/society/older-people">Older people</category>
      <category domain="https://www.theguardian.com/society/nhs">NHS</category>
      <pubDate>Sat, 01 Mar 2025 15:00:42 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/no-pain-all-gain-how-to-get-stronger-and-build-more-muscle</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3d642cc71fc98b3e936415802cfa0893f5f6937e/0_199_5976_3586/master/5976.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=af768c0f3379d31581f52e43e782884d">
        <media:credit scheme="urn:ebu">Photograph: Artist/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3d642cc71fc98b3e936415802cfa0893f5f6937e/0_199_5976_3586/master/5976.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d79b64fc17849a3aa4322cff0bf03a60">
        <media:credit scheme="urn:ebu">Photograph: Artist/Getty Images</media:credit>
      </media:content>
      <dc:creator>James Witts</dc:creator>
      <dc:date>2025-03-01T15:00:42Z</dc:date>
    </item>
    <item>
      <title>Coronation Street’s Kevin Kennedy looks back: ‘I collapsed after going two days without alcohol. I knew I’d die if I carried on’</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/coronation-streets-kevin-kennedy-looks-back-i-collapsed-after-going-two-days-without-alcohol-i-knew-id-die-if-i-carried-on</link>
      <description>&lt;p&gt;The actor on being ‘Curly’ Watts for 20 years, beating his addiction and playing the same festival as Johnny Cash&lt;/p&gt;&lt;p&gt;Born in 1961 in Wythenshawe, Manchester, Kevin Kennedy is best known for playing Norman “Curly” Watts on ITV&amp;nbsp;soap opera Coronation Street. The &amp;nbsp;Manchester Polytechnic graduate portrayed the supermarket worker from 1983 to 2003, as well as sustaining a career in music as a solo artist and in bands. He has appeared in musicals including We Will Rock You and&amp;nbsp;Rock of Ages, and stars in Punk Off – The Sounds of Punk and New Wave, which tours until 7 March.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;That &lt;/strong&gt;&lt;strong&gt;Barbour&lt;/strong&gt;&lt;strong&gt; jacket and I have been &lt;/strong&gt;through a lot together. Being Curly was&amp;nbsp;always comforting, like putting on&amp;nbsp;a pair of slippers you’ve worn for&amp;nbsp;years.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/coronation-streets-kevin-kennedy-looks-back-i-collapsed-after-going-two-days-without-alcohol-i-knew-id-die-if-i-carried-on"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/tv-and-radio/coronation-street">Coronation Street</category>
      <category domain="https://www.theguardian.com/tv-and-radio/soap-opera">Soap opera</category>
      <category domain="https://www.theguardian.com/culture/television">Television</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/tv-and-radio/drama">Drama</category>
      <category domain="https://www.theguardian.com/music/music">Music</category>
      <category domain="https://www.theguardian.com/media/simoncowell">Simon Cowell</category>
      <pubDate>Sat, 01 Mar 2025 12:00:37 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/coronation-streets-kevin-kennedy-looks-back-i-collapsed-after-going-two-days-without-alcohol-i-knew-id-die-if-i-carried-on</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/cbe057067de728f9eccd6cb7dab66258a6dcd8d4/163_1078_4322_2591/master/4322.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=679061ce223232a7463241d259267758">
        <media:credit scheme="urn:ebu">Photograph: Pål Hansen/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/cbe057067de728f9eccd6cb7dab66258a6dcd8d4/163_1078_4322_2591/master/4322.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b3f8d7e1f468a8049f09a21a4fbda783">
        <media:credit scheme="urn:ebu">Photograph: Pål Hansen/The Guardian</media:credit>
      </media:content>
      <dc:creator>Harriet Gibsone</dc:creator>
      <dc:date>2025-03-01T12:00:37Z</dc:date>
    </item>
    <item>
      <title>‘Smells a bit honky’: Rachel Roddy tests the best (and worst) supermarket pesto</title>
      <link>https://www.theguardian.com/thefilter/2025/mar/01/best-supermarket-pesto</link>
      <description>&lt;p&gt;Which jar boasts a flamboyant basil bouquet? And whose pesto tastes of gravel?&lt;/p&gt;&lt;p&gt;&lt;strong&gt;• &lt;a href="https://www.theguardian.com/food/article/2024/jul/10/the-best-kitchen-knives-for-every-job-chosen-by-chefs"&gt;The best kitchen knives for every job – chosen by chefs&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;It is true that pesto is an incredibly useful jar to have in the cupboard. It is also true that pesto is a hard thing to preserve in a jar in a way that tastes nice. This is especially true if you are working to keep costs down – as consumers, we can all do ballpark sums for the cost of herbs, nuts, cheese and olive oil. I think one of the problems lies with the herbs’ tendency to get a bit muggy during processing, though some makers are doing quite a good job.&lt;/p&gt;&lt;p&gt;Just to be clear about nomenclature: pesto, from the Latin &lt;em&gt;pestare&lt;/em&gt;, meaning to pound, is a generic word for a whole group of similar mixtures, the general guidelines being to mix any herb with any nut and with any cheese, then pour oil over the top. All the pestos I tasted fall into this general category, and they are called green pesto, Italian pesto or basil pesto. &lt;em&gt;Pesto alla Genovese&lt;/em&gt;, on the other hand, is a specific mixture of basil, pine nuts, garlic, parmesan, pecorino and olive oil.&lt;/p&gt; &lt;a href="https://www.theguardian.com/thefilter/2025/mar/01/best-supermarket-pesto"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/food/sauces-and-gravies">Sauces and gravies</category>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/food/pasta">Pasta</category>
      <category domain="https://www.theguardian.com/food/italian-food-and-drink">Italian food and drink</category>
      <pubDate>Sat, 01 Mar 2025 10:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/thefilter/2025/mar/01/best-supermarket-pesto</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/aeedbab6b5290e70f433f7a11b1a6f1574d58031/1404_1139_5087_3054/master/5087.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9e467074848885c96bd1ce2182ac8191">
        <media:credit scheme="urn:ebu">Photograph: Robert Billington/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/aeedbab6b5290e70f433f7a11b1a6f1574d58031/1404_1139_5087_3054/master/5087.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=535791e10e43a4c93910dcc7e8fc980b">
        <media:credit scheme="urn:ebu">Photograph: Robert Billington/The Guardian</media:credit>
      </media:content>
      <dc:creator>Rachel Roddy</dc:creator>
      <dc:date>2025-03-01T10:00:35Z</dc:date>
    </item>
    <item>
      <title>The moment I knew: I was snot-crying through a Disney film when he placed his head on my shoulder</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/02/the-moment-i-knew-i-was-snot-crying-through-a-disney-film-when-he-placed-his-head-on-my-shoulder</link>
      <description>&lt;p&gt;Watching a kids’ movie about self-acceptance reduced &lt;strong&gt;Will Hopkins&lt;/strong&gt; to a blubbering mess. But Herschel’s reaction made him feel safe and free from judgment&lt;/p&gt;&lt;ul&gt;&lt;li&gt;Find more stories from &lt;a href="https://www.theguardian.com/lifeandstyle/series/the-moment-i-knew"&gt;The moment I knew series here&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;In 2020, I was living in Sydney, working in real estate, attempting to finish my law degree on the side and struggling to juggle full-time work, full-time study and, admittedly, full-time partying. For a while I had been contemplating moving home to Brisbane to escape the chaos of Sydney but, at that time, chaos and I were old friends, and it was a friendship I thought would prevail my whole life.&lt;/p&gt;&lt;p&gt;I met Herschel on 20 December 2020 at about 10pm. I remember it well as it was also the day that a once firmly held belief of mine was shattered into oblivion.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/02/the-moment-i-knew-i-was-snot-crying-through-a-disney-film-when-he-placed-his-head-on-my-shoulder"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/relationships">Relationships</category>
      <category domain="https://www.theguardian.com/lifeandstyle/australian-lifestyle">Australian lifestyle</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 14:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/02/the-moment-i-knew-i-was-snot-crying-through-a-disney-film-when-he-placed-his-head-on-my-shoulder</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/ee43b8f598586eca28e9efd22d127e13a138cb04/0_140_1284_770/master/1284.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=30f1bcf01398ad8b4db333d61612acd9">
        <media:credit scheme="urn:ebu">Photograph: Will Hopkins</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/ee43b8f598586eca28e9efd22d127e13a138cb04/0_140_1284_770/master/1284.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=34f1ecb601398faf0e0ad0ada124804b">
        <media:credit scheme="urn:ebu">Photograph: Will Hopkins</media:credit>
      </media:content>
      <dc:creator>Will Hopkins</dc:creator>
      <dc:date>2025-03-01T14:00:44Z</dc:date>
    </item>
    <item>
      <title>This is how we do it: ‘I found it exciting imagining other women desiring my hot wife’</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/this-is-how-we-do-it-i-found-it-exciting-imagining-other-women-desiring-my-hot-wife</link>
      <description>&lt;p&gt;Monica and Garcie opened up their relationship – but only one of them is interested in extramarital dating&lt;/p&gt;&lt;p&gt;If anything, my affairs enrich our relationship. I feel more confident and sexy, and I bring that energy home&lt;/p&gt;&lt;p&gt;I helped set up Monica’s dating profile, and initially found it very exciting&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/this-is-how-we-do-it-i-found-it-exciting-imagining-other-women-desiring-my-hot-wife"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/sex">Sex</category>
      <category domain="https://www.theguardian.com/lifeandstyle/relationships">Relationships</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 13:21:25 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/this-is-how-we-do-it-i-found-it-exciting-imagining-other-women-desiring-my-hot-wife</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/38537a5c0e5abc3f1bfb7ea306ea29f086c226e1/8_0_1483_890/master/1483.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0b756ee86329b5d6734681c835eb2f17">
        <media:credit scheme="urn:ebu">Illustration: Ryan Gillett/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/38537a5c0e5abc3f1bfb7ea306ea29f086c226e1/8_0_1483_890/master/1483.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0d33b66488e769bf52d8d3af60251242">
        <media:credit scheme="urn:ebu">Illustration: Ryan Gillett/The Guardian</media:credit>
      </media:content>
      <dc:creator>Interviews by Kitty Drake</dc:creator>
      <dc:date>2025-03-01T13:21:25Z</dc:date>
    </item>
    <item>
      <title>Bournemouth v Wolves goes to extra time: FA Cup latest, EFL, La Liga and more – live</title>
      <link>https://www.theguardian.com/football/live/2025/mar/01/bournemouth-v-wolves-fa-cup-latest-plus-efl-bundesliga-la-liga-and-more-live</link>
      <description>&lt;ul&gt;&lt;li&gt;Updates from Saturday’s afternoon kick-offs&lt;/li&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/football/live"&gt;Live scoreboard&lt;/a&gt; | &lt;a href="mailto:will.magee@theguardian.com"&gt;Email Will&lt;/a&gt; | &lt;a href="https://www.theguardian.com/info/2022/nov/14/football-daily-email-sign-up"&gt;Sign up for Football Daily&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;&lt;strong&gt;There’s been another goal at Selhurst Park.&lt;/strong&gt; Palace are now 3-1 up, Eddie Nketiah getting his fourth goal of the season.&lt;/p&gt;&lt;p&gt;It’s official: Preston are through to the FA Cup quarters for the first time in 59 years. There’s bound to be considerable fallout from the match but, on the field, there’s little doubt the hosts were the dominant team.&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/live/2025/mar/01/bournemouth-v-wolves-fa-cup-latest-plus-efl-bundesliga-la-liga-and-more-live"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/football/fa-cup">FA Cup</category>
      <category domain="https://www.theguardian.com/football/championship">Championship</category>
      <category domain="https://www.theguardian.com/football/laligafootball">La Liga</category>
      <category domain="https://www.theguardian.com/football/bundesligafootball">Bundesliga</category>
      <category domain="https://www.theguardian.com/football/serieafootball">Serie A</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <category domain="https://www.theguardian.com/football/europeanfootball">European club football</category>
      <pubDate>Sat, 01 Mar 2025 17:09:27 GMT</pubDate>
      <guid>https://www.theguardian.com/football/live/2025/mar/01/bournemouth-v-wolves-fa-cup-latest-plus-efl-bundesliga-la-liga-and-more-live</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/416f43bfc40974cbae2eda9cfda4b4c811150d3f/0_372_5533_3321/master/5533.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=748948b894e545f6647e32633ac56475">
        <media:credit scheme="urn:ebu">Photograph: Peter Cziborra/Action Images/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/416f43bfc40974cbae2eda9cfda4b4c811150d3f/0_372_5533_3321/master/5533.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f70226f1b497fb4d4d144c72eb430f0a">
        <media:credit scheme="urn:ebu">Photograph: Peter Cziborra/Action Images/Reuters</media:credit>
      </media:content>
      <dc:creator>Will Magee</dc:creator>
      <dc:date>2025-03-01T17:09:27Z</dc:date>
    </item>
    <item>
      <title>Manchester City v Plymouth Argyle: FA Cup fifth round – live</title>
      <link>https://www.theguardian.com/football/live/2025/mar/01/manchester-city-v-plymouth-argyle-fa-cup-fifth-round-live-score-updates</link>
      <description>&lt;ul&gt;&lt;li&gt;FA Cup fifth round news from the 5.45pm GMT kick-off&lt;/li&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/football/live"&gt;Live scores&lt;/a&gt; | &lt;a href="https://www.theguardian.com/football/series/footballdaily"&gt;Read the latest Football Daily&lt;/a&gt; | &lt;a href="mailto:barry.glendenning@theguardian.com"&gt;Mail Barry&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;With almost 7,800 Plymouth fans expected to make the journey to the Etihad for a tie that must be decided tonight following the scrapping of FA Cup replays, their team’s Northern Irish goalkeeper Conor Hazard has stressed the importance of his side doing their best in the face of extremely daunting opposition and will be well prepared for a penalty shootout should the outcome be determined by spot-kicks.&lt;/p&gt;&lt;p&gt;“You’ve got to kind of have an idea what you’re going to be coming up against, what’s their preferred side and what to do,” he said. “We’ll try to keep the game going as long as possible and there’s every chance a game like this could go to penalties. We’ll definitely do some preparation on that.&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/live/2025/mar/01/manchester-city-v-plymouth-argyle-fa-cup-fifth-round-live-score-updates"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/fa-cup">FA Cup</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/football/manchestercity">Manchester City</category>
      <category domain="https://www.theguardian.com/football/plymouthargyle">Plymouth Argyle</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <pubDate>Sat, 01 Mar 2025 17:12:24 GMT</pubDate>
      <guid>https://www.theguardian.com/football/live/2025/mar/01/manchester-city-v-plymouth-argyle-fa-cup-fifth-round-live-score-updates</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/52640978cbd714b4ff34128450cc5c0e7cc35ed0/0_267_7996_4798/master/7996.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a25d734cb5afa0338f48036772181dd5">
        <media:credit scheme="urn:ebu">Photograph: Godfrey Pitt/Action Plus/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/52640978cbd714b4ff34128450cc5c0e7cc35ed0/0_267_7996_4798/master/7996.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3c9286b8eb520d40ba35e71541ce08fc">
        <media:credit scheme="urn:ebu">Photograph: Godfrey Pitt/Action Plus/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Barry Glendenning</dc:creator>
      <dc:date>2025-03-01T17:12:24Z</dc:date>
    </item>
    <item>
      <title>Crystal Palace through after Millwall keeper’s red for head-high tackle on Mateta</title>
      <link>https://www.theguardian.com/football/2025/mar/01/crystal-palace-millwall-fa-cup-match-report</link>
      <description>&lt;p&gt;Crystal Palace progressed to the quarter-finals of the FA Cup at a sun-kissed, slightly distracted Selhust Park. A win against opponents who played with 10 men for 85 minutes was comfortable enough. Sadly, this south London derby will be remembered instead for a single outstanding act of violence.&lt;/p&gt;&lt;p&gt;It took place inside the stadium, and in front of the watching millions, as Liam Roberts launched an adrenal, wildly dangerous flying challenge into the head of Jean-Philippe Mateta with five minutes gone. In the process, Roberts killed his own continued participation in the game, plus a large portion of the BBC live broadcast, and could conceivably have done the same to Mateta.&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/2025/mar/01/crystal-palace-millwall-fa-cup-match-report"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/fa-cup">FA Cup</category>
      <category domain="https://www.theguardian.com/football/crystalpalace">Crystal Palace</category>
      <category domain="https://www.theguardian.com/football/millwall">Millwall</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <pubDate>Sat, 01 Mar 2025 14:35:31 GMT</pubDate>
      <guid>https://www.theguardian.com/football/2025/mar/01/crystal-palace-millwall-fa-cup-match-report</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/7697d457b5d3d23c12ece27721cd31302f400c0f/265_174_2049_1229/master/2049.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=98d7f2d0dad0f96185e6d3c30c5873eb">
        <media:credit scheme="urn:ebu">Photograph: Jacques Feeney/Offside/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/7697d457b5d3d23c12ece27721cd31302f400c0f/265_174_2049_1229/master/2049.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b49f74e3d66f4826dfad3d20bfb0b944">
        <media:credit scheme="urn:ebu">Photograph: Jacques Feeney/Offside/Getty Images</media:credit>
      </media:content>
      <dc:creator>Barney Ronay at Selhurst Park</dc:creator>
      <dc:date>2025-03-01T14:35:31Z</dc:date>
    </item>
    <item>
      <title>Jos Buttler’s England reign ends in painful defeat by South Africa</title>
      <link>https://www.theguardian.com/sport/2025/mar/01/england-south-africa-champions-trophy-cricket</link>
      <description>&lt;ul&gt;&lt;li&gt;Gp B: &lt;a href="https://www.theguardian.com/sport/cricket/match/2025-03-01/england-cricket-team"&gt;South Africa, 181-3, bt England, 179, by 7 wkts&lt;/a&gt;&lt;/li&gt;&lt;li&gt;England limp out of Champions Trophy with three losses&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Jos Buttler called the Champions Trophy a brutal format before the start of the tournament but in the end it proved mercifully short. Had this been a World Cup, stretched out over weeks to milk even more television money, England could easily have surpassed that bleak title defence in India 18 months ago by way of ignominy.&lt;/p&gt;&lt;p&gt;Instead, it was off to the airport, via the hotel, after this third and final defeat; the first time they have exited a global event without a single group win. South Africa, who topped Group B with a crushing seven-wicket win, were due to fly to Dubai on Sunday, even if the farcical schedule – dictated by India having to play all their matches in the gulf state – meant they do not know whether this will actually be the scene of their semi-final.&lt;/p&gt; &lt;a href="https://www.theguardian.com/sport/2025/mar/01/england-south-africa-champions-trophy-cricket"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/sport/iccchampionstrophy">ICC Champions Trophy</category>
      <category domain="https://www.theguardian.com/sport/south-africa-cricket-team">South Africa cricket team</category>
      <category domain="https://www.theguardian.com/sport/england-cricket-team">England cricket team</category>
      <category domain="https://www.theguardian.com/sport/cricket">Cricket</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <category domain="https://www.theguardian.com/sport/jos-buttler">Jos Buttler</category>
      <pubDate>Sat, 01 Mar 2025 15:16:00 GMT</pubDate>
      <guid>https://www.theguardian.com/sport/2025/mar/01/england-south-africa-champions-trophy-cricket</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/064877b5b6bea27e8877b54b6e6ab415269e4d6d/0_230_4674_2805/master/4674.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=8f64a17008a6c26487247be32651f621">
        <media:credit scheme="urn:ebu">Photograph: Akhtar Soomro/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/064877b5b6bea27e8877b54b6e6ab415269e4d6d/0_230_4674_2805/master/4674.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=26118db5824aca47afd696776a55dac6">
        <media:credit scheme="urn:ebu">Photograph: Akhtar Soomro/Reuters</media:credit>
      </media:content>
      <dc:creator>Ali Martin at the National Stadium</dc:creator>
      <dc:date>2025-03-01T15:16:00Z</dc:date>
    </item>
    <item>
      <title>Goalkeepers to be punished with corner for holding ball more than eight seconds</title>
      <link>https://www.theguardian.com/football/2025/mar/01/goalkeepers-to-be-punished-with-corner-for-holding-ball-more-than-eight-seconds</link>
      <description>&lt;ul&gt;&lt;li&gt;Ifab announces law change for next season&lt;/li&gt;&lt;li&gt;‘It could be one of those very effective deterrents’&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Goalkeepers who waste time by holding on to the ball are to be penalised with the award of a corner, the law-making International Football Association Board (Ifab) has confirmed.&lt;/p&gt;&lt;p&gt;The new law, which will be rolled out across the game this summer, will mean goalkeepers have eight seconds to claim and redistribute the ball before they are penalised, with the referee giving the keeper a five-second countdown to warn them of incoming punishment. This will replace the current system whereby a keeper has six seconds to move the ball on, and is punished with an indirect free-kick if they do not.&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/2025/mar/01/goalkeepers-to-be-punished-with-corner-for-holding-ball-more-than-eight-seconds"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/laws-of-football">Laws of football</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <pubDate>Sat, 01 Mar 2025 17:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/football/2025/mar/01/goalkeepers-to-be-punished-with-corner-for-holding-ball-more-than-eight-seconds</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/66c38f42d428c41ca2c49c50de64909766f23487/106_2250_1319_791/master/1319.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=1a0690c8754bf1b27ea8b339a7f47069">
        <media:credit scheme="urn:ebu">Photograph: Robbie Jay Barratt/AMA/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/66c38f42d428c41ca2c49c50de64909766f23487/106_2250_1319_791/master/1319.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ebfe312ab0dafc3aebb76f5f147a15f1">
        <media:credit scheme="urn:ebu">Photograph: Robbie Jay Barratt/AMA/Getty Images</media:credit>
      </media:content>
      <dc:creator>Paul MacInnes in Belfast</dc:creator>
      <dc:date>2025-03-01T17:00:44Z</dc:date>
    </item>
    <item>
      <title>Preston’s Milutin Osmajic riles Burnley with Cup goal taunts after racism claim</title>
      <link>https://www.theguardian.com/football/2025/mar/01/preston-burnley-fa-cup-match-report</link>
      <description>&lt;p&gt;The PA announcer was stating the obvious but, nevertheless, “Preston advance to the FA Cup quarter-finals” was met with a tumultuous roar from the home faithful. No Preston fan has heard that line since 1966 but a dominant display made it a reality. Milutin Osmajic, almost inevitability, was central to the dismantling of Burnley.&lt;/p&gt;&lt;p&gt;Osmajic &lt;a href="https://www.theguardian.com/football/2025/feb/17/fa-looking-into-hannibal-mejbri-racism-allegation-against-preston-milutin-osmajic"&gt;was accused of racially abusing the Burnley midfielder Hannibal Mejbri&lt;/a&gt; when the teams met here a fortnight ago. He scored Preston’s second and repaid the taunts from the away fans by cupping his ears in celebration in front of them. Robbie Brady had ignited the tie with a superb free-kick and Will Keane sealed a deserved victory after a fine team&amp;nbsp;move.&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/2025/mar/01/preston-burnley-fa-cup-match-report"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/fa-cup">FA Cup</category>
      <category domain="https://www.theguardian.com/football/preston">Preston North End</category>
      <category domain="https://www.theguardian.com/football/burnley">Burnley</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <pubDate>Sat, 01 Mar 2025 14:22:47 GMT</pubDate>
      <guid>https://www.theguardian.com/football/2025/mar/01/preston-burnley-fa-cup-match-report</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/609d4119357d10b32c05e15bfabd766cc5778d45/0_0_4296_2578/master/4296.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4275c984521ea981ab1b0d9eda798bf1">
        <media:credit scheme="urn:ebu">Photograph: Paul Ellis/AFP/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/609d4119357d10b32c05e15bfabd766cc5778d45/0_0_4296_2578/master/4296.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=76dc80574e694dcbc7926698d049dce2">
        <media:credit scheme="urn:ebu">Photograph: Paul Ellis/AFP/Getty Images</media:credit>
      </media:content>
      <dc:creator>Andy Hunter at Deepdale</dc:creator>
      <dc:date>2025-03-01T14:22:47Z</dc:date>
    </item>
    <item>
      <title>Championship: West Brom halt Leeds run and Weimann saves Blackburn</title>
      <link>https://www.theguardian.com/football/2025/mar/01/championship-west-brom-leeds-darnell-furlong</link>
      <description>&lt;ul&gt;&lt;li&gt;Furlong equaliser frustrates leaders at home&lt;/li&gt;&lt;li&gt;Two late goals provide drama at Ewood Park&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;&lt;strong&gt;Leeds&lt;/strong&gt;’ Championship title charge was checked as they were held to a frustrating 1-1 draw at Elland Road by &lt;strong&gt;West Brom&lt;/strong&gt;. Junior Firpo headed the home side into an early lead but Darnell Furlong equalised with a looping header before the break.&lt;/p&gt;&lt;p&gt;Leeds were denied a sixth straight win but extended their unbeaten league run to 17 matches. West Brom’s winless league run on the road extended to 10 games. Leeds are now eight points clear of Burnley, who sit third and &lt;a href="https://www.theguardian.com/football/2025/mar/01/preston-burnley-fa-cup-match-report"&gt;lost at Preston in the FA Cup&lt;/a&gt;, while second-placed Sheffield United play at QPR in a 3pm kick-off.&lt;/p&gt;&lt;p&gt;&lt;em&gt;This story will be updated&lt;/em&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/2025/mar/01/championship-west-brom-leeds-darnell-furlong"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/championship">Championship</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <category domain="https://www.theguardian.com/football/leedsunited">Leeds United</category>
      <category domain="https://www.theguardian.com/football/westbrom">West Bromwich Albion</category>
      <category domain="https://www.theguardian.com/football/blackburn">Blackburn Rovers</category>
      <category domain="https://www.theguardian.com/football/norwichcity">Norwich City</category>
      <category domain="https://www.theguardian.com/football/coventry">Coventry City</category>
      <category domain="https://www.theguardian.com/football/oxford-united">Oxford United</category>
      <pubDate>Sat, 01 Mar 2025 15:33:30 GMT</pubDate>
      <guid>https://www.theguardian.com/football/2025/mar/01/championship-west-brom-leeds-darnell-furlong</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/45d9525ca44f68b822097d1fbf9d0a8c61a7feda/0_48_4336_2601/master/4336.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=159f4ac72f081ba5bd15b3837136b200">
        <media:credit scheme="urn:ebu">Photograph: Adam Fradgley/West Bromwich Albion FC/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/45d9525ca44f68b822097d1fbf9d0a8c61a7feda/0_48_4336_2601/master/4336.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9117402ad5185db4a4bbe4f3286dec56">
        <media:credit scheme="urn:ebu">Photograph: Adam Fradgley/West Bromwich Albion FC/Getty Images</media:credit>
      </media:content>
      <dc:creator>PA Media</dc:creator>
      <dc:date>2025-03-01T15:33:30Z</dc:date>
    </item>
    <item>
      <title>Fulham’s Sander Berge: ‘I can be more brutal, more nasty … you need that’</title>
      <link>https://www.theguardian.com/football/2025/mar/01/fulham-sander-berge-norway-fa-cup</link>
      <description>&lt;p&gt;As he prepares to face Manchester United in the FA Cup on Sunday, the midfielder talks tactics, up and downs and being part of Norway’s ‘little golden generation’&lt;/p&gt;&lt;p&gt;As Fulham finalise their preparations for Sunday’s trip to Manchester United in the fifth round of the FA Cup, Sander Berge is thinking about how to handle adversity. “It taught me a lot about myself,” the midfielder says, recalling how spirits could have sagged when he was part of unsuccessful fights for survival with Burnley and Sheffield United.&lt;/p&gt;&lt;p&gt;“When you’re struggling every day it’s difficult. That’s a time to show character more than ever. You demand the ball, you take pride in going out there and showing you have the ability to stay at that level. It’s about who you want to be.”&lt;/p&gt; &lt;a href="https://www.theguardian.com/football/2025/mar/01/fulham-sander-berge-norway-fa-cup"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/football/fulham">Fulham</category>
      <category domain="https://www.theguardian.com/football/fa-cup">FA Cup</category>
      <category domain="https://www.theguardian.com/football/norway">Norway</category>
      <category domain="https://www.theguardian.com/football/football">Football</category>
      <category domain="https://www.theguardian.com/sport/sport">Sport</category>
      <pubDate>Sat, 01 Mar 2025 13:22:00 GMT</pubDate>
      <guid>https://www.theguardian.com/football/2025/mar/01/fulham-sander-berge-norway-fa-cup</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5a4e2e408a51b3a67ef7d6d1887fc733d271a01f/0_321_4767_2860/master/4767.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=948f4a525513329f2e7000d196e0a5fb">
        <media:credit scheme="urn:ebu">Photograph: Andy Hall/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5a4e2e408a51b3a67ef7d6d1887fc733d271a01f/0_321_4767_2860/master/4767.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c9acc8a46f584ee8f59692cd4fa78924">
        <media:credit scheme="urn:ebu">Photograph: Andy Hall/The Observer</media:credit>
      </media:content>
      <dc:creator>Jacob Steinberg</dc:creator>
      <dc:date>2025-03-01T13:22:00Z</dc:date>
    </item>
    <item>
      <title>Trump’s style of petty domination was in full display with Zelenskyy | Moira Donegan</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/trumps-style-of-petty-domination-was-in-full-display-with-zelenskyy</link>
      <description>&lt;p&gt;Trump and Vance, I think, never really intended to have a conversation with Zelenskky. Instead, they wanted to look tough on TV&lt;/p&gt;&lt;p&gt;The last time Donald Trump did this, it was in secret, and he got impeached over it. In 2019, Donald Trump, on a phone call with Volodymyr Zelenskyy, demanded that the Ukrainian president produce – or fabricate – evidence of wrongdoing by Hunter Biden, the son of Trump’s eventual opponent in the 2020 election, in exchange for continued US military aide.&lt;/p&gt;&lt;p&gt;At the time, Russia had already seized control of the Ukrainian region of Crimea, and was funding violent insurgent groups in the country’s east; it was increasingly clear that a full-scale Russian invasion was coming, as it finally did in 2022. Since the end of second world war, it has been America that checks Russian expansionist ambitions in Europe – America that provided the backstop to the Nato alliance, America that secured the independence of eastern Europe. Trump wanted to condition that longstanding role on Zelenskyy doing him a personal political favor. The international order could be ended, he suggested, if those who depended on him didn’t do enough to indulge his vanity, self-interest and impulsive whims.&lt;/p&gt;&lt;p&gt;Moira Donegan is a Guardian US columnist&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/trumps-style-of-petty-domination-was-in-full-display-with-zelenskyy"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <pubDate>Sat, 01 Mar 2025 17:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/trumps-style-of-petty-domination-was-in-full-display-with-zelenskyy</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/af33a8e1f72049596190acd42c238c59ad9dcb43/0_0_4741_2845/master/4741.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=52e63fdfdcfb9e0d4d4932596c35ead4">
        <media:credit scheme="urn:ebu">Photograph: Brian Snyder/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/af33a8e1f72049596190acd42c238c59ad9dcb43/0_0_4741_2845/master/4741.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f5900afa60cce3c3b0e922bb73db7aac">
        <media:credit scheme="urn:ebu">Photograph: Brian Snyder/Reuters</media:credit>
      </media:content>
      <dc:creator>Moira Donegan</dc:creator>
      <dc:date>2025-03-01T17:00:44Z</dc:date>
    </item>
    <item>
      <title>The dogma of ‘Britain’s Strictest Headmistress’ is a con as old as time - gentle parenting produces happier kids</title>
      <link>https://www.theguardian.com/society/2025/mar/01/the-dogma-of-britains-strictest-headmistress-is-a-con-as-old-as-time-gentle-parenting-produces-happier-kids</link>
      <description>&lt;p&gt;Disciplinarians claim a stern approach is best, but there’s a lot of evidence to dispute this, says the Observer parenting columnist&lt;/p&gt;&lt;p&gt;You’ve heard the ­terrible news, I’m sure. Our children are pampered. We raise the coddled brats not as stern parents but simpering friends. We flatter their whims and let them bury their heads in screens. We fetishise what they feel, care not for what they learn, and neglect what they need: that good old-fashioned commonsense discipline that raised the great generations of times past.&lt;/p&gt;&lt;p&gt;Inarguably the greatest peddler of this diagnosis is Katharine Birbalsingh, &lt;a href="https://www.theguardian.com/education/2022/may/22/uks-strictest-headmistress-fears-schools-will-stop-teaching-shakespeare"&gt;Britain’s Strictest Headmistress&lt;/a&gt;™ and co-founder of the Michaela Community School in Wembley, which boasts fastidious adherence to uniforms, timed loo breaks and silent corridors. In an interview with the &lt;em&gt;Times&lt;/em&gt; last week, she yet again bemoaned the “­gentle parenting” that is leaving her students ill-equipped for modern life.&lt;/p&gt; &lt;a href="https://www.theguardian.com/society/2025/mar/01/the-dogma-of-britains-strictest-headmistress-is-a-con-as-old-as-time-gentle-parenting-produces-happier-kids"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/society/children">Children</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <category domain="https://www.theguardian.com/lifeandstyle/parents-and-parenting">Parents and parenting</category>
      <category domain="https://www.theguardian.com/education/education">Education</category>
      <category domain="https://www.theguardian.com/lifeandstyle/family">Family</category>
      <pubDate>Sat, 01 Mar 2025 15:36:41 GMT</pubDate>
      <guid>https://www.theguardian.com/society/2025/mar/01/the-dogma-of-britains-strictest-headmistress-is-a-con-as-old-as-time-gentle-parenting-produces-happier-kids</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c4fbb7fd3cdad885730860604f47fe6a23813cad/0_353_6629_3977/master/6629.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=704efe01d5196ad3790b24dc39239b83">
        <media:credit scheme="urn:ebu">Photograph: Sam Pelly/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c4fbb7fd3cdad885730860604f47fe6a23813cad/0_353_6629_3977/master/6629.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e2c38bc1b0775bf7d385aeb4484925c9">
        <media:credit scheme="urn:ebu">Photograph: Sam Pelly/The Observer</media:credit>
      </media:content>
      <dc:creator>Séamas O’Reilly</dc:creator>
      <dc:date>2025-03-01T15:36:41Z</dc:date>
    </item>
    <item>
      <title>With Nato adrift and Brussels snubbed, is the UK key to Europe’s response to Trump?| Simon Tisdall</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/britain-europe-response-donald-trump-nato-brussels</link>
      <description>&lt;p&gt;In a fast-moving crisis, the EU hasn’t been nimble enough. The onus must fall on ‘coalitions of the willing’ to stop a US-Putin carve-up&lt;/p&gt;&lt;p&gt;At moments of great crisis, national leaders and governments generally put their countries’ (and their own) interests first. Transnational geopolitical, economic and security alliances are all very well. But if such organisations do not or cannot rise to the urgent challenges of the day, they risk being bypassed, ignored or shunted aside. This is the predicament now facing the European Union.&lt;/p&gt;&lt;p&gt;After Donald Trump’s appalling treatment of Volodymyr Zelenskyy in full view of the watching world on Friday night, all agree that the US president’s &lt;a href="https://www.theguardian.com/commentisfree/2025/feb/22/from-saviour-to-judas-how-trumps-pivot-on-russia-also-endangers-his-own-country"&gt;betrayal of Ukraine&lt;/a&gt;, sickening embrace of Russia and his blunt demand that Europe henceforth defend itself represent just such an extraordinary challenge, and one that must be swiftly addressed.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/britain-europe-response-donald-trump-nato-brussels"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/eu">European Union</category>
      <category domain="https://www.theguardian.com/world/nato">Nato</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Sat, 01 Mar 2025 16:30:42 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/britain-europe-response-donald-trump-nato-brussels</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/33e888e763813bf95497ffdef677d8e05345a3c0/2_0_10663_6400/master/10663.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2d8a06f1776a94e3240bfa7106c9792a">
        <media:credit scheme="urn:ebu">Illustration: Dominic McKenzie/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/33e888e763813bf95497ffdef677d8e05345a3c0/2_0_10663_6400/master/10663.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=5874119eaee3e965e5180e33508922c6">
        <media:credit scheme="urn:ebu">Illustration: Dominic McKenzie/The Observer</media:credit>
      </media:content>
      <dc:creator>Simon Tisdall</dc:creator>
      <dc:date>2025-03-01T16:30:42Z</dc:date>
    </item>
    <item>
      <title>Even rightwingers are mocking the ‘Epstein files’ as a lot of redacted nothing</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/epstein-files-trump-administration</link>
      <description>&lt;p&gt;It was yet another reminder that Trump and his associates will turn even the sex trafficking of minors into a photo op&lt;/p&gt;&lt;p&gt;Drum roll, please: the “most transparent administration in American history” is declassifying shocking new &lt;a href="https://www.theguardian.com/us-news/2025/feb/27/jeffrey-epstein-files-released"&gt;information about Jeffrey Epstein&lt;/a&gt; and his associates. After years of speculation that powerful people have been concealing information related to the late financier and convicted sex offender, the Trump administration said earlier this week that it would release unseen details about the case.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/epstein-files-trump-administration"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/us-news/jeffrey-epstein">Jeffrey Epstein</category>
      <category domain="https://www.theguardian.com/us-news/pam-bondi">Pam Bondi</category>
      <category domain="https://www.theguardian.com/lifeandstyle/women">Women</category>
      <category domain="https://www.theguardian.com/media/media">Media</category>
      <category domain="https://www.theguardian.com/society/women">Women</category>
      <category domain="https://www.theguardian.com/world/gender">Gender</category>
      <category domain="https://www.theguardian.com/world/feminism">Feminism</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <category domain="https://www.theguardian.com/us-news/ghislaine-maxwell">Ghislaine Maxwell</category>
      <category domain="https://www.theguardian.com/news/andrew-tate">Andrew Tate</category>
      <category domain="https://www.theguardian.com/world/abortion">Abortion</category>
      <category domain="https://www.theguardian.com/world/capital-punishment">Capital punishment</category>
      <category domain="https://www.theguardian.com/society/children">Children</category>
      <category domain="https://www.theguardian.com/world/gaza">Gaza</category>
      <category domain="https://www.theguardian.com/technology/jeff-bezos">Jeff Bezos</category>
      <category domain="https://www.theguardian.com/music/katy-perry">Katy Perry</category>
      <category domain="https://www.theguardian.com/science/space-shuttle">The space shuttle</category>
      <category domain="https://www.theguardian.com/science/space">Space</category>
      <category domain="https://www.theguardian.com/society/contraception-and-family-planning">Contraception and family planning</category>
      <category domain="https://www.theguardian.com/tv-and-radio/peppa-pig">Peppa Pig</category>
      <category domain="https://www.theguardian.com/music/cardi-b">Cardi B</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 14:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/epstein-files-trump-administration</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c2e740386a7f6a918471bde8eba76e228c1ac5d1/0_20_3063_1838/master/3063.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ff9660cd07581f9e412e0221c0091a73">
        <media:credit scheme="urn:ebu">Photograph: Kevin Lamarque/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c2e740386a7f6a918471bde8eba76e228c1ac5d1/0_20_3063_1838/master/3063.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=6996e61d835d0acd6812b8a78bfc82a7">
        <media:credit scheme="urn:ebu">Photograph: Kevin Lamarque/Reuters</media:credit>
      </media:content>
      <dc:creator>Arwa Mahdawi</dc:creator>
      <dc:date>2025-03-01T14:00:43Z</dc:date>
    </item>
    <item>
      <title>It might be a small consolation, but Elon Musk is getting poorer by the day | John Naughton</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/it-might-be-a-small-consolation-but-elon-musk-is-getting-poorer-by-the-day</link>
      <description>&lt;p&gt;As his goons root through the innards of the US government, Tesla sales are plummeting, the cars are being defaced and owners are ashamed&lt;/p&gt;&lt;p&gt;Extreme wealth has always played a role in democracies. Money has always talked, especially in the US. Years ago, Lawrence Lessig, the great legal scholar, calculated that most of the campaign funding for members of Congress and aspiring politicians came from one-twentieth of the richest 1% of Americans – about 150,000 people. This is about the same number as those who are named “Lester” &lt;a href="https://lessig.org/product/the-usa-is-lesterland/"&gt;and explains the title of his book&lt;/a&gt;: &lt;em&gt;The USA &lt;/em&gt;&lt;em&gt;Is Lesterland&lt;/em&gt;.&lt;/p&gt;&lt;p&gt;But that particular corruption of American politics only involved billionaires like the Koch brothers playing organ-grinders to congressional monkeys. The obscene wealth generated by the tech industry has catapulted a new organ-grinder into the heart of the machine. He was able to pay his way in with a spare quarter of a billion dollars that he happened to have lying around. And now the wretched citizens of the US find themselves living in Muskland.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/it-might-be-a-small-consolation-but-elon-musk-is-getting-poorer-by-the-day"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/technology/elon-musk">Elon Musk</category>
      <category domain="https://www.theguardian.com/technology/tesla">Tesla</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <pubDate>Sat, 01 Mar 2025 16:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/it-might-be-a-small-consolation-but-elon-musk-is-getting-poorer-by-the-day</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/cb6221157520b61e3a3de2c1d4722774f277fba8/97_0_1557_934/master/1557.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a1a9efe5a4bbbe9756e7b974983d5c17">
        <media:credit scheme="urn:ebu">Photograph: Instagram / @everyonehateselon_</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/cb6221157520b61e3a3de2c1d4722774f277fba8/97_0_1557_934/master/1557.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b6b49059f9e2f8b0f43273e646b06e71">
        <media:credit scheme="urn:ebu">Photograph: Instagram / @everyonehateselon_</media:credit>
      </media:content>
      <dc:creator>John Naughton</dc:creator>
      <dc:date>2025-03-01T16:00:44Z</dc:date>
    </item>
    <item>
      <title>Jeff Bezos takes one small step for feminism and social progress, and one giant leap for self-publicity  | Sarah Manavis</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/jeff-bezos-feminism-flight-space-amazon-blue-origin</link>
      <description>&lt;p&gt;The first all-female private flight into space funded by the Amazon tycoon’s Blue Origin company has little to do with female empowerment and a lot to do with PR &lt;/p&gt;&lt;p&gt;A pop star, a TV host and a billionaire’s fiancée walk into a private rocket ship. The pop star turns to the others and asks: “Is this what feminism looks like?” According to the space technology company Blue Origin, owned and founded by the Amazon tycoon Jeff Bezos, the answer seems to be a resounding yes.&lt;/p&gt;&lt;p&gt;On Thursday Blue Origin announced it would be launching the &lt;a href="https://www.theguardian.com/science/2025/feb/27/blue-origin-all-woman-crew-flight"&gt;first-ever all-female commercial flight to space&lt;/a&gt; with a crew of astronauts including US singer Katy Perry, the morning news host (– and close friend of Oprah Winfrey – Gayle King and Bezos’s own partner, the journalist Lauren Sánchez, who reportedly organised the mission, which will happen sometime this spring.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/jeff-bezos-feminism-flight-space-amazon-blue-origin"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/science/space">Space</category>
      <category domain="https://www.theguardian.com/technology/jeff-bezos">Jeff Bezos</category>
      <category domain="https://www.theguardian.com/science/science">Science</category>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <category domain="https://www.theguardian.com/science/blue-origin">Blue Origin</category>
      <category domain="https://www.theguardian.com/music/katy-perry">Katy Perry</category>
      <category domain="https://www.theguardian.com/technology/amazon">Amazon</category>
      <category domain="https://www.theguardian.com/media/washington-post">Washington Post</category>
      <category domain="https://www.theguardian.com/world/feminism">Feminism</category>
      <category domain="https://www.theguardian.com/science/spacex">SpaceX</category>
      <category domain="https://www.theguardian.com/technology/elon-musk">Elon Musk</category>
      <category domain="https://www.theguardian.com/science/virgin-galactic">Virgin Galactic</category>
      <category domain="https://www.theguardian.com/business/richard-branson">Richard Branson</category>
      <category domain="https://www.theguardian.com/media/newspapers">Newspapers</category>
      <category domain="https://www.theguardian.com/culture/william-shatner">William Shatner</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/lifeandstyle/women">Women</category>
      <pubDate>Sat, 01 Mar 2025 13:00:39 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/jeff-bezos-feminism-flight-space-amazon-blue-origin</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/9976c647de00b4ca268f82bf931cb76c092e49ba/94_0_2813_1688/master/2813.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=69de28da657f93ee5a043a282f0dad47">
        <media:credit scheme="urn:ebu">Photograph: Blue Origin Handout/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/9976c647de00b4ca268f82bf931cb76c092e49ba/94_0_2813_1688/master/2813.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9d9d6582a1dd6e9c0e765aa42803a9c3">
        <media:credit scheme="urn:ebu">Photograph: Blue Origin Handout/EPA</media:credit>
      </media:content>
      <dc:creator>Sarah Manavis</dc:creator>
      <dc:date>2025-03-01T13:00:39Z</dc:date>
    </item>
    <item>
      <title>Five years on from the pandemic, how has Covid changed our world? | The panel</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/five-years-covid-first-lockdown-lives-changed</link>
      <description>&lt;p&gt;We asked a group of experts on politics, trade, literature, psychology, work and more: what has been the most surprising or shocking consequence of Covid-19 in your field? &lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/five-years-covid-first-lockdown-lives-changed"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/coronavirus-outbreak">Coronavirus</category>
      <category domain="https://www.theguardian.com/business/internationaltrade">International trade</category>
      <category domain="https://www.theguardian.com/books/books">Books</category>
      <category domain="https://www.theguardian.com/world/disability">Disability</category>
      <category domain="https://www.theguardian.com/society/women">Women</category>
      <category domain="https://www.theguardian.com/money/work-and-careers">Work &amp; careers</category>
      <category domain="https://www.theguardian.com/science/psychology">Psychology</category>
      <category domain="https://www.theguardian.com/society/nhs">NHS</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/education/education">Education</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/science/infectiousdiseases">Infectious diseases</category>
      <pubDate>Sat, 01 Mar 2025 09:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/five-years-covid-first-lockdown-lives-changed</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/f26bd31b3086a879ed631b46893f23b01c88024d/0_127_2596_1558/master/2596.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e63b70897f82149fde7d20d13ecd92f5">
        <media:credit scheme="urn:ebu">Illustration: Eleanor Shakespeare/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/f26bd31b3086a879ed631b46893f23b01c88024d/0_127_2596_1558/master/2596.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=34b5d276129bc596012eb59377f014f6">
        <media:credit scheme="urn:ebu">Illustration: Eleanor Shakespeare/The Guardian</media:credit>
      </media:content>
      <dc:creator>Stephen Reicher, Rachel Clarke, Rafael Behr,  Frances Ryan and others</dc:creator>
      <dc:date>2025-03-01T09:00:35Z</dc:date>
    </item>
    <item>
      <title>With lists and notebooks, I find that I am worryingly on the same page as Elon Musk | Rachel Cooke</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/lists-notebooks-elon-musk</link>
      <description>&lt;p&gt;Asking federal staff to bullet point their achievements would be easier to scorn, were my own to-do tallying not so compulsive&lt;/p&gt;&lt;p&gt;Watching &lt;a href="https://www.theguardian.com/politics/video/2025/feb/27/starmer-gives-trump-invitation-from-king-charles-for-uk-state-visit-video"&gt;Keir Starmer with President Trump&lt;/a&gt; in Washington last week was a bit like watching an indulgent grandparent deal with a miscreant child. When the prime minister produced his invitation from King Charles – “This is unprecedented!” he said delightedly, of what will be Don’s second state visit to the UK – I half expected him to follow up with a Lego model of the White House, or a special Trump Pez dispenser and a year’s supply of cola-flavoured sweets for it.&lt;/p&gt;&lt;p&gt;Alas, I’m unable to be equally scornful of Elon Musk’s edict to federal employees that they tell him in an email of five things they accomplished in the last week. Oh yes, it’s silly. Who’ll look through these, and how will they check the enclosed bullet points aren’t the work of the office satirist? But as a compulsive list-maker myself, my outrage is on the muted side. Sheepishly, I shuffle my notebooks, their closely written pages so replete with determination, wild ambition and pathos, I come off like some tragic hybrid of Adrian Mole and Martha Stewart.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/lists-notebooks-elon-musk"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/technology/elon-musk">Elon Musk</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/artanddesign/national-portrait-gallery">National Portrait Gallery</category>
      <pubDate>Sat, 01 Mar 2025 16:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/lists-notebooks-elon-musk</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/6d820b58a22b066b6ba6e85338e2ee6841ed9075/0_0_2960_1775/master/2960.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=49a857ee34b9564bfcaedc69fab2a0d0">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/6d820b58a22b066b6ba6e85338e2ee6841ed9075/0_0_2960_1775/master/2960.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0d0a5b6b53b0afa239880a145ce2efe7">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <dc:creator>Rachel Cooke</dc:creator>
      <dc:date>2025-03-01T16:00:44Z</dc:date>
    </item>
    <item>
      <title>The Guardian view on PM’s gamble: exploiting crisis to remake Labour was a step too far for an ally | Editorial</title>
      <link>https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-pms-gamble-exploiting-crisis-to-remake-labour-was-a-step-too-far-for-an-ally</link>
      <description>&lt;p&gt;The exit of a soft-left intellectual politician from government highlights a growing unease about the reordering of the party’s priorities&lt;/p&gt;&lt;p&gt;The resignation of &lt;a href="https://www.theguardian.com/politics/2025/feb/28/anneliese-dodds-resigns-keir-starmer-cut-aid-budget"&gt;Anneliese Dodds&lt;/a&gt;, the international development minister, from Labour’s cabinet may not have been entirely unexpected. Sir Keir Starmer’s decision to cut the aid budget to “pay” for increased defence spending was wrong. Making the world’s poorest foot the bill for Britain’s security is reckless and&amp;nbsp;self-defeating. Slashing aid fuels instability – it won’t&amp;nbsp;buy&amp;nbsp;safety. From her perch in government Ms Dodds, who was Sir Keir’s first shadow chancellor, knew&amp;nbsp;this better than most.&lt;/p&gt;&lt;p&gt;The former cabinet minister’s letter is right to warn that the cuts will mean the UK withdrawing from many developing countries and having a diminished role in global institutions like the World Bank, the G7 and climate negotiations. She pointedly argued Britain will find it “impossible” to deliver on its commitment to maintain development spending in Gaza, Sudan and Ukraine with the shrunken budget. Sir Keir rebuffed this charge, but Ms Dodds is right to say his move is being seen as following the Trumpian lead in cutting &lt;a href="https://www.theguardian.com/us-news/2025/feb/26/trump-usaid-cuts"&gt;USAid&lt;/a&gt; – a framing that implies the UK is losing its independent foreign policy direction.&lt;/p&gt;&lt;p&gt;&lt;em&gt;&lt;strong&gt;Do you have an opinion on the issues raised in this article? If you would like to submit a response of up to 300 words by email to be considered for publication in our&lt;a href="https://www.theguardian.com/tone/letters"&gt; letters&lt;/a&gt; section, please &lt;a href="mailto:guardian.letters@theguardian.com?body=Please%20include%20your%20name,%20full%20postal%20address%20and%20phone%20number%20with%20your%20letter%20below.%20Letters%20are%20usually%20published%20with%20the%20author%27s%20name%20and%20city/town/village.%20The%20rest%20of%20the%20information%20is%20for%20verification%20only%20and%20to%20contact%20you%20where%20necessary."&gt;click here&lt;/a&gt;.&lt;/strong&gt;&lt;/em&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-pms-gamble-exploiting-crisis-to-remake-labour-was-a-step-too-far-for-an-ally"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/politics/labour">Labour</category>
      <category domain="https://www.theguardian.com/global-development/aid">Aid</category>
      <category domain="https://www.theguardian.com/politics/anneliese-dodds">Anneliese Dodds</category>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/politics/defence">Defence policy</category>
      <pubDate>Fri, 28 Feb 2025 18:57:55 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-pms-gamble-exploiting-crisis-to-remake-labour-was-a-step-too-far-for-an-ally</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5435e4d1d7b4e5d58cff4a403dab6927a5fd16b4/0_106_4453_2672/master/4453.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4ed7ca35a03f16755b1bc994a4c41f66">
        <media:credit scheme="urn:ebu">Photograph: Thomas Krych/ZUMA Press Wire/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5435e4d1d7b4e5d58cff4a403dab6927a5fd16b4/0_106_4453_2672/master/4453.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f4e65c0f06adec9a766a89bdce89c591">
        <media:credit scheme="urn:ebu">Photograph: Thomas Krych/ZUMA Press Wire/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Editorial</dc:creator>
      <dc:date>2025-02-28T18:57:55Z</dc:date>
    </item>
    <item>
      <title>The Guardian view on Britain and the US: Starmer spoke Trump’s language, but it’s deeds that matter | Editorial</title>
      <link>https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-zelenskyy-in-washington-trump-turns-his-fire-on-the-beleaguered-president</link>
      <description>&lt;p&gt;Hopes of swaying the White House on Ukraine look bleak as US prioritised theatrics over security commitments in a volatile world&lt;/p&gt;&lt;p&gt;Europe hoped that concerted efforts could have some effect in bringing round Donald Trump to a more reasonable position on Ukraine, and mitigate the worst of his administration’s instincts. After the combined persuasion and flattery of &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/keir-starmer-trump-us-uk-relationship"&gt;Sir Keir Starmer&lt;/a&gt; and Emmanuel Macron brought out a somewhat tamer and more jovial version of the US president, there were modest hopes that the Ukrainian president’s visit to Washington might be more productive than feared – even if there was no sign that they had succeeded in tempting Mr Trump towards the security assurances so desperately needed.&lt;/p&gt;&lt;p&gt;Instead, his Oval Office meeting with Volodymyr Zelenskyy soon exploded into acrimony, with the US president berating his guest for ingratitude. Mr Trump had earlier spoken of partnership with the Ukrainian president. But he and his vice-president, JD Vance, teamed up to deliver a public kicking. It would, the president added, be “great television”.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-zelenskyy-in-washington-trump-turns-his-fire-on-the-beleaguered-president"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/world/emmanuel-macron">Emmanuel Macron</category>
      <category domain="https://www.theguardian.com/world/eu">European Union</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <category domain="https://www.theguardian.com/world/gaza">Gaza</category>
      <category domain="https://www.theguardian.com/us-news/marco-rubio">Marco Rubio</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <pubDate>Fri, 28 Feb 2025 18:59:06 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/feb/28/the-guardian-view-on-zelenskyy-in-washington-trump-turns-his-fire-on-the-beleaguered-president</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/14082ef52856a3270737e7f07434ec97c5f29525/0_509_7626_4577/master/7626.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c97aaa071029c23f462ee8a16380801c">
        <media:credit scheme="urn:ebu">Photograph: Jim Lo Scalzo/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/14082ef52856a3270737e7f07434ec97c5f29525/0_509_7626_4577/master/7626.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c0f43d455fe2f69105b203af4a0e658c">
        <media:credit scheme="urn:ebu">Photograph: Jim Lo Scalzo/EPA</media:credit>
      </media:content>
      <dc:creator>Editorial</dc:creator>
      <dc:date>2025-02-28T18:59:06Z</dc:date>
    </item>
    <item>
      <title>Boosting public funding is the only way to make the arts more inclusive | Letters</title>
      <link>https://www.theguardian.com/education/2025/feb/28/boosting-public-funding-is-the-only-way-to-make-the-arts-more-inclusive</link>
      <description>&lt;p&gt;Readers respond to Guardian analysis on how the arts sector is still a barrier for working-class people&lt;/p&gt;&lt;p&gt;Your article (&lt;a href="https://www.theguardian.com/culture/2025/feb/21/working-class-creatives-dont-stand-a-chance-in-uk-today-leading-artists-warn"&gt;Working-class creatives don’t stand a chance in UK today, leading artists warn, 21 February&lt;/a&gt;) suggests that the higher percentage of privately educated people in leadership roles in the arts is due to a “rigged system” that shuts out working-class people, yet, despite highlighting the fall in students taking arts and humanities subjects, it fails to draw the obvious conclusion.&lt;/p&gt;&lt;p&gt;When provision of arts tuition in the state sector has almost disappeared, young people who are unable to pay for private tuition and whose schools don’t have art or drama departments are hugely disadvantaged from the outset if they wish for a career in the arts. How can children explore and gain confidence in their creative potential if they can’t test it in an art department, music room or on an assembly hall stage?&lt;/p&gt; &lt;a href="https://www.theguardian.com/education/2025/feb/28/boosting-public-funding-is-the-only-way-to-make-the-arts-more-inclusive"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/education/arts">Arts</category>
      <category domain="https://www.theguardian.com/inequality/class-issues">Class issues</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/society/children">Children</category>
      <category domain="https://www.theguardian.com/stage/theatre">Theatre</category>
      <category domain="https://www.theguardian.com/stage/stage">Stage</category>
      <category domain="https://www.theguardian.com/inequality/inequality">Inequality</category>
      <category domain="https://www.theguardian.com/education/schools">Schools</category>
      <category domain="https://www.theguardian.com/education/private-schools">Private schools</category>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <category domain="https://www.theguardian.com/politics/labour">Labour</category>
      <category domain="https://www.theguardian.com/politics/lisa-nandy">Lisa Nandy</category>
      <category domain="https://www.theguardian.com/politics/johnprescott">John Prescott</category>
      <category domain="https://www.theguardian.com/education/dramaanddance">Drama and dance</category>
      <category domain="https://www.theguardian.com/education/education">Education</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Fri, 28 Feb 2025 17:17:45 GMT</pubDate>
      <guid>https://www.theguardian.com/education/2025/feb/28/boosting-public-funding-is-the-only-way-to-make-the-arts-more-inclusive</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c0f6f1ed1468e50afba890d37ca0adfffd23e66d/0_177_5022_3013/master/5022.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=68454fd54ad0168c5f2d5dc78f3d4bee">
        <media:credit scheme="urn:ebu">Photograph: Alex Segre/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c0f6f1ed1468e50afba890d37ca0adfffd23e66d/0_177_5022_3013/master/5022.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=335a54ec12fffbb8e958be183189cd0a">
        <media:credit scheme="urn:ebu">Photograph: Alex Segre/Alamy</media:credit>
      </media:content>
      <dc:creator>Guardian Staff</dc:creator>
      <dc:date>2025-02-28T17:17:45Z</dc:date>
    </item>
    <item>
      <title>When Britain was hot on manufacturing – and kettle design | Letter</title>
      <link>https://www.theguardian.com/business/2025/feb/28/when-britain-was-hot-on-manufacturing-and-kettle-design</link>
      <description>&lt;p&gt;&lt;strong&gt;Dr Nicholas Russell &lt;/strong&gt;responds to a review by Edward Posnett and reflects on the pioneering work of his father and his business partner, Peter Hobbs&lt;/p&gt;&lt;p&gt;I was pleased to read &lt;a href="https://www.theguardian.com/books/2025/feb/19/your-life-is-manufactured-by-tim-minshall-review-object-lessons"&gt;Edward Posnett’s review&lt;/a&gt; (19 February) of Tim Minshall’s Your Life Is Manufactured . Little attention is currently paid to manufacturing, perhaps because it comprises only &lt;a href="https://commonslibrary.parliament.uk/research-briefings/sn05206/"&gt;8%&lt;/a&gt; of British GDP.&lt;/p&gt;&lt;p&gt;The opening of the review focuses on the domestic kettle, Posnett emphasising that the “tocks” of automatic kettles switching off are as significant for us as the nightingale’s song was for John Keats. But despite this cultural significance, no electric kettles are made in Britain now.&lt;/p&gt; &lt;a href="https://www.theguardian.com/business/2025/feb/28/when-britain-was-hot-on-manufacturing-and-kettle-design"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/business/manufacturing-sector">Manufacturing sector</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <category domain="https://www.theguardian.com/books/scienceandnature">Science and nature books</category>
      <category domain="https://www.theguardian.com/books/books">Books</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/food/tea">Tea</category>
      <category domain="https://www.theguardian.com/artanddesign/design">Design</category>
      <category domain="https://www.theguardian.com/law/intellectual-property">Intellectual property</category>
      <pubDate>Fri, 28 Feb 2025 17:07:13 GMT</pubDate>
      <guid>https://www.theguardian.com/business/2025/feb/28/when-britain-was-hot-on-manufacturing-and-kettle-design</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/f2dfadcad192fbf2d95b4ed489cecac0fcee8e6d/0_116_3840_2304/master/3840.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=89e6127b2f40d62f564c61f4eceaad4f">
        <media:credit scheme="urn:ebu">Photograph: Wavebreakmedia Ltd UC9/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/f2dfadcad192fbf2d95b4ed489cecac0fcee8e6d/0_116_3840_2304/master/3840.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a9173e936671d6d52c04dbcc2a5e1940">
        <media:credit scheme="urn:ebu">Photograph: Wavebreakmedia Ltd UC9/Alamy</media:credit>
      </media:content>
      <dc:creator>Guardian Staff</dc:creator>
      <dc:date>2025-02-28T17:07:13Z</dc:date>
    </item>
    <item>
      <title>Three essential things to know about microplastics – and how to avoid them</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/how-to-avoid-microplastics</link>
      <description>&lt;p&gt;Microplastics can’t be avoided completely, but even small steps in the right direction can help significantly&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/lifeandstyle/2025/jan/24/sign-up-for-the-detox-your-kitchen-newsletter-your-guide-to-avoiding-chemicals-in-your-food"&gt;Sign up for our Detox Your Kitchen newsletter&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;On a recent trip to New Orleans, the king cake baby became, for me, a symbol of plastic’s ubiquity in the food system. &lt;a href="https://www.theguardian.com/lifeandstyle/wordofmouth/2010/feb/16/mardi-gras-pancake-king-cake"&gt;King cakes&lt;/a&gt; are a beloved Mardi Gras season sweet, and when bakers are done cooking them, they hide a small plastic baby in each. Whoever gets a slice with the baby in it receives good luck in the coming year.&lt;/p&gt;&lt;p&gt;I write about toxic chemicals for a living, so when I learned about the tradition, I let out a small groan while estimating how many microplastics the baby must be shedding into the cake.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/how-to-avoid-microplastics"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/environment/plastic">Plastics</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 14:00:40 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/how-to-avoid-microplastics</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/336a90d7bbdf9bd310f64a161be25d30705efc2b/0_173_5184_3110/master/5184.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e6df491e9ee495a007de150ea5e91a2c">
        <media:credit scheme="urn:ebu">Photograph: Kinga Krzeminska/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/336a90d7bbdf9bd310f64a161be25d30705efc2b/0_173_5184_3110/master/5184.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=fd36361fb941687a5667843f1f8a9ffa">
        <media:credit scheme="urn:ebu">Photograph: Kinga Krzeminska/Getty Images</media:credit>
      </media:content>
      <dc:creator>Tom Perkins</dc:creator>
      <dc:date>2025-03-01T14:00:40Z</dc:date>
    </item>
    <item>
      <title>Japan battles largest wildfire in decades</title>
      <link>https://www.theguardian.com/world/2025/mar/01/japan-battles-largest-wildfire-in-decades</link>
      <description>&lt;p&gt;More than a thousand people have been evacuated near forest of Ofunato in northern region of Iwate&lt;/p&gt;&lt;p&gt;More than a thousand people have been evacuated as Japan battles its largest wildfire in more than three decades.&lt;/p&gt;&lt;p&gt;The flames are estimated to have spread over about 1,200 hectares (3,000 acres) in the forest of Ofunato in the northern region of Iwate since a fire broke out on Wednesday, according to the Fire and Disaster Management Agency.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/japan-battles-largest-wildfire-in-decades"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/japan">Japan</category>
      <category domain="https://www.theguardian.com/world/wildfires">Wildfires</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/asia-pacific">Asia Pacific</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/environment/climate-crisis">Climate crisis</category>
      <pubDate>Sat, 01 Mar 2025 10:22:26 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/japan-battles-largest-wildfire-in-decades</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/afcdcd86d45a60d9741d3119d61da91d04239a5b/0_169_2048_1229/master/2048.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3562ea5108ab427332c514b415239af4">
        <media:credit scheme="urn:ebu">Photograph: Ofunato City/JIJI PRESS/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/afcdcd86d45a60d9741d3119d61da91d04239a5b/0_169_2048_1229/master/2048.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a25d1751f983c4b352520a98b9fbbdfc">
        <media:credit scheme="urn:ebu">Photograph: Ofunato City/JIJI PRESS/EPA</media:credit>
      </media:content>
      <dc:creator>Agence France-Presse in Tokyo</dc:creator>
      <dc:date>2025-03-01T10:22:26Z</dc:date>
    </item>
    <item>
      <title>‘Ultimate bringers of life’: How one Cornwall farmer is using beavers to stop flooding</title>
      <link>https://www.theguardian.com/environment/2025/feb/28/ultimate-bringers-of-life-cornwall-farmer-beavers-stop-flooding</link>
      <description>&lt;p&gt;Chris Jones is behind change in law to release beavers in England after witnessing the incredible benefits on his land&lt;/p&gt;&lt;p&gt;• &lt;a href="https://www.theguardian.com/environment/2025/feb/28/beavers-released-english-waterways-government-licence"&gt;Beaver releases into wild to be allowed in England for first time in centuries&lt;/a&gt;&lt;/p&gt;&lt;p&gt;Chris Jones, a beef farmer, is very proud of his beavers. “They are just extraordinary,” he says.&lt;/p&gt;&lt;p&gt;Since releasing a couple into an enclosure on his Cornwall farm in 2017, he says they have saved it from drought, prevented flooding in the nearby village, boosted the local economy and even improved oyster beds in Falmouth Bay.&lt;/p&gt; &lt;a href="https://www.theguardian.com/environment/2025/feb/28/ultimate-bringers-of-life-cornwall-farmer-beavers-stop-flooding"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/environment/wildlife">Wildlife</category>
      <category domain="https://www.theguardian.com/environment/conservation">Conservation</category>
      <category domain="https://www.theguardian.com/environment/mammals">Mammals</category>
      <category domain="https://www.theguardian.com/environment/farming">Farming</category>
      <category domain="https://www.theguardian.com/environment/rivers">Rivers</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/uk-news/cornwall">Cornwall</category>
      <category domain="https://www.theguardian.com/environment/flooding">Flooding</category>
      <category domain="https://www.theguardian.com/environment/drought">Drought</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/environment/water">Water</category>
      <category domain="https://www.theguardian.com/business/cattles">Cattle</category>
      <category domain="https://www.theguardian.com/environment/environment-agency">Environment Agency</category>
      <pubDate>Fri, 28 Feb 2025 13:29:24 GMT</pubDate>
      <guid>https://www.theguardian.com/environment/2025/feb/28/ultimate-bringers-of-life-cornwall-farmer-beavers-stop-flooding</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/9f7b990a75f059efcc9742453a69a7eee73d80b0/0_153_4596_2758/master/4596.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e1f2e2a106e5e2a9512276fe2ac2bce4">
        <media:credit scheme="urn:ebu">Photograph: Josh Hariss</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/9f7b990a75f059efcc9742453a69a7eee73d80b0/0_153_4596_2758/master/4596.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=bfe14407e56de260c5c70cd278eb8ca0">
        <media:credit scheme="urn:ebu">Photograph: Josh Hariss</media:credit>
      </media:content>
      <dc:creator>Helena Horton Environment reporter</dc:creator>
      <dc:date>2025-02-28T13:29:24Z</dc:date>
    </item>
    <item>
      <title>Weather tracker: six cyclones swirl simultaneously in southern hemisphere</title>
      <link>https://www.theguardian.com/environment/2025/feb/28/weather-tracker-six-cyclones-southern-hemisphere-alfred</link>
      <description>&lt;p&gt;Bianca, Garance and Honde churn across Indian Ocean as Alfred, Rae and Seru spin through south-west Pacific&lt;/p&gt;&lt;p&gt;An uncommon meteorological event unfolded on Tuesday when six named tropical cyclones were active simultaneously in the southern hemisphere, several in close proximity to one another.&lt;/p&gt;&lt;p&gt;Three developed in the south-west Pacific. Severe Tropical Cyclone Alfred formed on 20 February in the Coral Sea to the north-east of Australia, reaching an intensity equivalent to a category 4 hurricane on Thursday with sustained winds of 105mph (170km/h) and gusts at about 140mph.&lt;/p&gt; &lt;a href="https://www.theguardian.com/environment/2025/feb/28/weather-tracker-six-cyclones-southern-hemisphere-alfred"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/australia-news/australia-news">Australia news</category>
      <category domain="https://www.theguardian.com/australia-news/queensland">Queensland</category>
      <category domain="https://www.theguardian.com/world/madagascar">Madagascar</category>
      <category domain="https://www.theguardian.com/world/pacific-islands">Pacific islands</category>
      <category domain="https://www.theguardian.com/world/solomonislands">Solomon Islands</category>
      <category domain="https://www.theguardian.com/world/mozambique">Mozambique</category>
      <category domain="https://www.theguardian.com/world/africa">Africa</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/asia-pacific">Asia Pacific</category>
      <category domain="https://www.theguardian.com/world/hurricanes">Hurricanes</category>
      <pubDate>Fri, 28 Feb 2025 10:55:13 GMT</pubDate>
      <guid>https://www.theguardian.com/environment/2025/feb/28/weather-tracker-six-cyclones-southern-hemisphere-alfred</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d74c10d083fe6d4aa72320a808e1c906bda001a0/0_176_2648_1589/master/2648.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=52bdc90b59f167a7c3a89b45a16fc137">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d74c10d083fe6d4aa72320a808e1c906bda001a0/0_176_2648_1589/master/2648.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f57615147937b0959fd92b71046abc05">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <dc:creator>Lauren Herdman for MetDesk</dc:creator>
      <dc:date>2025-02-28T10:55:13Z</dc:date>
    </item>
    <item>
      <title>Labour steps up attacks on Farage and Reform over pro-Russia stance</title>
      <link>https://www.theguardian.com/politics/2025/mar/01/labour-steps-up-attacks-on-farage-and-reform-over-pro-russia-stance</link>
      <description>&lt;p&gt;Government targets party’s ‘softness on standing up to Putin’ to show Reform is out of step with UK public&lt;/p&gt;&lt;p&gt;Labour is setting out to increase its attacks on Nigel Farage’s Reform UK over its stance on Russia, as polling and focus groups show the public are firmly pro-Ukraine and against Vladimir Putin.&lt;/p&gt;&lt;p&gt;One cabinet source said Labour was planning to “take the fight” to Reform on the issues of the Ukraine war and the NHS after “waking up” to the party’s “softness on standing up to Putin”.&lt;/p&gt; &lt;a href="https://www.theguardian.com/politics/2025/mar/01/labour-steps-up-attacks-on-farage-and-reform-over-pro-russia-stance"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/politics/labour">Labour</category>
      <category domain="https://www.theguardian.com/politics/nigel-farage">Nigel Farage</category>
      <category domain="https://www.theguardian.com/politics/brexit-party">Reform UK</category>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/vladimir-putin">Vladimir Putin</category>
      <pubDate>Sat, 01 Mar 2025 06:00:30 GMT</pubDate>
      <guid>https://www.theguardian.com/politics/2025/mar/01/labour-steps-up-attacks-on-farage-and-reform-over-pro-russia-stance</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b339f102adf9560d1d5a9fab852d6a6c411e84b1/0_201_3443_2066/master/3443.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b593045ab8de294e5c6cf2215f7cb4f9">
        <media:credit scheme="urn:ebu">Photograph: Adam Vaughan/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b339f102adf9560d1d5a9fab852d6a6c411e84b1/0_201_3443_2066/master/3443.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a49094c271658b140ca1ff2ab69ee761">
        <media:credit scheme="urn:ebu">Photograph: Adam Vaughan/EPA</media:credit>
      </media:content>
      <dc:creator>Rowena Mason and Hannah Al-Othman</dc:creator>
      <dc:date>2025-03-01T06:00:30Z</dc:date>
    </item>
    <item>
      <title>Five teenagers arrested after 17-year-old boy stabbed in Nottingham</title>
      <link>https://www.theguardian.com/uk-news/2025/mar/01/five-teenagers-arrested-after-17-year-old-boy-stabbed-in-nottingham</link>
      <description>&lt;p&gt;Two other teenagers were also injured in incident at what police believe was a flat party&lt;/p&gt;&lt;p&gt;Five teenagers have been arrested after a 17-year-old boy was stabbed at what police believe was a flat party.&lt;/p&gt;&lt;p&gt;Two other people, a 17-year-old boy and a 16-year-old girl, were also injured in the incident.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/mar/01/five-teenagers-arrested-after-17-year-old-boy-stabbed-in-nottingham"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Sat, 01 Mar 2025 15:49:03 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/mar/01/five-teenagers-arrested-after-17-year-old-boy-stabbed-in-nottingham</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c9571475a74d58e7280c46440c3f0bc5377a1d43/0_161_4928_2957/master/4928.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=32b56bca26eba2f3d2b91c9611935ce6">
        <media:credit scheme="urn:ebu">Photograph: George Sweeney/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c9571475a74d58e7280c46440c3f0bc5377a1d43/0_161_4928_2957/master/4928.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c0538bf2f5c6f943351c95eb0f19f080">
        <media:credit scheme="urn:ebu">Photograph: George Sweeney/Alamy</media:credit>
      </media:content>
      <dc:creator>Hayden Vernon</dc:creator>
      <dc:date>2025-03-01T15:49:03Z</dc:date>
    </item>
    <item>
      <title>Ramadan display lights up Piccadilly Circus in London</title>
      <link>https://www.theguardian.com/uk-news/2025/mar/01/ramadan-display-lights-up-piccadilly-circus-in-london</link>
      <description>&lt;p&gt;The city’s mayor, Sadiq Khan, led the celebrations to observe holy month of Ramadan, now in their third year&lt;/p&gt;&lt;p&gt;Piccadilly Circus has once again been lit up by an installation to mark Ramadan.&lt;/p&gt;&lt;p&gt;It is the third year of the annual display, which features 30,000 LED bulbs in the shape of Islamic geometric patterns and symbols hanging over the West End street.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/mar/01/ramadan-display-lights-up-piccadilly-circus-in-london"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk/london">London</category>
      <category domain="https://www.theguardian.com/politics/sadiq-khan">Sadiq Khan</category>
      <category domain="https://www.theguardian.com/world/islam">Islam</category>
      <category domain="https://www.theguardian.com/world/ramadan">Ramadan</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/uk-news/england">England</category>
      <category domain="https://www.theguardian.com/world/religion">Religion</category>
      <category domain="https://www.theguardian.com/cities/cities">Cities</category>
      <pubDate>Sat, 01 Mar 2025 08:51:17 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/mar/01/ramadan-display-lights-up-piccadilly-circus-in-london</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/1a86af290a7195dfe805e200f9a25362496ed72f/0_468_8016_4810/master/8016.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b354aa1a926d96af17f7b47ea99fd814">
        <media:credit scheme="urn:ebu">Photograph: Tolga Akmen/EPA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/1a86af290a7195dfe805e200f9a25362496ed72f/0_468_8016_4810/master/8016.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=11f6603557c8c2b88a57173c9f829bc4">
        <media:credit scheme="urn:ebu">Photograph: Tolga Akmen/EPA</media:credit>
      </media:content>
      <dc:creator>Mariam Amini</dc:creator>
      <dc:date>2025-03-01T08:51:17Z</dc:date>
    </item>
    <item>
      <title>Land’s End lighthouse fog alarm sounding every 13 seconds</title>
      <link>https://www.theguardian.com/uk-news/2025/feb/28/cornish-lighthouses-fog-alarm-has-been-going-off-every-13-seconds-for-past-week</link>
      <description>&lt;p&gt;Buy a set of earplugs, maritime charity advises those hoping to sleep near Cornwall’s Longships Lighthouse &lt;/p&gt;&lt;p&gt;The distant sound of a lighthouse can be a part of coastal life that really adds to the maritime ambience.&lt;/p&gt;&lt;p&gt;For those living near Land’s End, however, any sense of whimsy has worn off. A fog alarm at Longships Lighthouse, just off the Cornish headland, has been going off every 13 seconds for the past week.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/feb/28/cornish-lighthouses-fog-alarm-has-been-going-off-every-13-seconds-for-past-week"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk-news/cornwall">Cornwall</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Fri, 28 Feb 2025 17:50:16 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/feb/28/cornish-lighthouses-fog-alarm-has-been-going-off-every-13-seconds-for-past-week</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/73770322450d9e9a0392717a9f68b35922e25c05/41_0_3427_2057/master/3427.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d1799bde22bc504c768087ea25e178c2">
        <media:credit scheme="urn:ebu">Photograph: Andrew Matthews/PA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/73770322450d9e9a0392717a9f68b35922e25c05/41_0_3427_2057/master/3427.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=74b6d19571b6cb4faeb2a650fb18a567">
        <media:credit scheme="urn:ebu">Photograph: Andrew Matthews/PA</media:credit>
      </media:content>
      <dc:creator>Sammy Gecsoyler</dc:creator>
      <dc:date>2025-02-28T17:50:16Z</dc:date>
    </item>
    <item>
      <title>British pharma company GSK pauses diversity work citing Trump orders</title>
      <link>https://www.theguardian.com/business/2025/feb/28/uk-based-british-pharma-gsk-pauses-diversity-work-citing-trump-orders</link>
      <description>&lt;p&gt;London-based FTSE 100 firm reviewing its policies, saying it is obliged to comply because US is its No 1 market&lt;/p&gt;&lt;p&gt;The British pharma company GSK has paused diversity activities for UK workers, claiming that it is obliged to do so in response to executive orders by the US president, Donald Trump.&lt;/p&gt;&lt;p&gt;The FTSE 100 company has also scrubbed references to “diversity” from its website. GSK is led by Emma Walmsley, one of the few women to head a FTSE 100 company.&lt;/p&gt; &lt;a href="https://www.theguardian.com/business/2025/feb/28/uk-based-british-pharma-gsk-pauses-diversity-work-citing-trump-orders"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/business/glaxosmithkline">GSK</category>
      <category domain="https://www.theguardian.com/business/pharmaceuticals-industry">Pharmaceuticals industry</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <pubDate>Fri, 28 Feb 2025 17:42:03 GMT</pubDate>
      <guid>https://www.theguardian.com/business/2025/feb/28/uk-based-british-pharma-gsk-pauses-diversity-work-citing-trump-orders</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/2096ec93a207f71f5b120b6d5f5515c65db79e27/2_272_2657_1595/master/2657.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4ec4b02717e96e349ccbe691a19ef138">
        <media:credit scheme="urn:ebu">Photograph: Olivier Hess Commissioned By Gsk/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/2096ec93a207f71f5b120b6d5f5515c65db79e27/2_272_2657_1595/master/2657.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=aaa72b7038a1cd99fdd62ed9f023e086">
        <media:credit scheme="urn:ebu">Photograph: Olivier Hess Commissioned By Gsk/Reuters</media:credit>
      </media:content>
      <dc:creator>Jasper Jolly</dc:creator>
      <dc:date>2025-02-28T17:42:03Z</dc:date>
    </item>
    <item>
      <title>Extreme online violence may be linked to rise of ‘0 to 100’ killers, experts say</title>
      <link>https://www.theguardian.com/technology/2025/mar/01/extreme-online-violence-internet-links-experts</link>
      <description>&lt;p&gt;Criminal justice specialists call for new approach to identify emerging type of murderer with no prior convictions&lt;/p&gt;&lt;p&gt;The rise of “0 to 100” killers who go from watching torture, mutilation and beheading videos in their bedrooms to committing murder suggests there could be a link between extreme violence online and in real life, experts have said.&lt;/p&gt;&lt;p&gt;Criminal justice experts advocated a new approach, inspired by counter-terrorism, to identify an emerging type of murderer with no prior convictions, after cases such as Nicholas Prosper, who killed his mother and siblings and planned a primary school massacre.&lt;/p&gt; &lt;a href="https://www.theguardian.com/technology/2025/mar/01/extreme-online-violence-internet-links-experts"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/technology/internet-safety">Internet safety</category>
      <category domain="https://www.theguardian.com/society/youngpeople">Young people</category>
      <category domain="https://www.theguardian.com/law/criminal-justice">UK criminal justice</category>
      <category domain="https://www.theguardian.com/technology/internet">Internet</category>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <category domain="https://www.theguardian.com/uk-news/prevent-strategy">Prevent strategy</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <category domain="https://www.theguardian.com/uk/uksecurity">UK security and counter-terrorism</category>
      <category domain="https://www.theguardian.com/society/childprotection">Child protection</category>
      <pubDate>Sat, 01 Mar 2025 08:00:34 GMT</pubDate>
      <guid>https://www.theguardian.com/technology/2025/mar/01/extreme-online-violence-internet-links-experts</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/0ced37c8fedeff0fee9783fb9c6efac26eed1504/557_636_4164_2499/master/4164.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=90860a22eb3d85253e5c6f6f314e9188">
        <media:credit scheme="urn:ebu">Photograph: Artur Marciniec/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/0ced37c8fedeff0fee9783fb9c6efac26eed1504/557_636_4164_2499/master/4164.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3f571c3b570c606707f5a79d419131de">
        <media:credit scheme="urn:ebu">Photograph: Artur Marciniec/Alamy</media:credit>
      </media:content>
      <dc:creator>Rachel Hall</dc:creator>
      <dc:date>2025-03-01T08:00:34Z</dc:date>
    </item>
    <item>
      <title>Starmer may have weaponised the Windsors, but soft power is the royals’ great asset</title>
      <link>https://www.theguardian.com/uk-news/2025/mar/01/starmer-may-have-weaponised-the-windsors-but-soft-power-is-the-royals-great-asset</link>
      <description>&lt;p&gt;Just as in the Oval Office this week, history shows the royals can be deployed to serve British interests – whether they like it or not&lt;/p&gt;&lt;p&gt;As Donald Trump &lt;a href="https://www.theguardian.com/us-news/2025/feb/27/king-charles-invites-donald-trump-for-unprecedented-second-state-visit-to-uk"&gt;waved his personal invitation from King Charles III&lt;/a&gt; to pay a second historic state visit in the Oval Office, there was no disguising his delight before the TV cameras.&lt;/p&gt;&lt;p&gt;Keir Starmer had retrieved the letter from his jacket pocket and handed it to the US president with the dramatic flourish of Neville Chamberlain’s “I have in my hand a piece of paper” moment.&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/2025/mar/01/starmer-may-have-weaponised-the-windsors-but-soft-power-is-the-royals-great-asset"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk/monarchy">Monarchy</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <pubDate>Sat, 01 Mar 2025 07:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/2025/mar/01/starmer-may-have-weaponised-the-windsors-but-soft-power-is-the-royals-great-asset</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/8ffbfde6fcbc063618cb70558e51910a234668d4/0_1_6808_4085/master/6808.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ec0415f853473656385e437191bc0485">
        <media:credit scheme="urn:ebu">Photograph: ABACA/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/8ffbfde6fcbc063618cb70558e51910a234668d4/0_1_6808_4085/master/6808.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=282c0588fa23c809fef57ae2d3494e5e">
        <media:credit scheme="urn:ebu">Photograph: ABACA/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Caroline Davies</dc:creator>
      <dc:date>2025-03-01T07:00:35Z</dc:date>
    </item>
    <item>
      <title>Tickled pink: rhubarb growers see explosion in demand for Yorkshire crop</title>
      <link>https://www.theguardian.com/food/2025/mar/01/tickled-pink-rhubarb-growers-see-explosion-in-demand-for-yorkshire-crop</link>
      <description>&lt;p&gt;Despite wet weather hitting yields, supermarkets are reporting a doubling in rhubarb sales compared to last year&lt;/p&gt;&lt;p&gt;It takes a while for the eyes to adjust to the darkness inside the shed. Slowly, the shapes of hundreds of pale stalks emerge from the gloom like an alien species, visible only by the glow cast by a handful of candles.&lt;/p&gt;&lt;p&gt;This candlelit ritual is the harvest of Yorkshire forced rhubarb, being carried out by growers Robert and Paula Tomlinson.&lt;/p&gt; &lt;a href="https://www.theguardian.com/food/2025/mar/01/tickled-pink-rhubarb-growers-see-explosion-in-demand-for-yorkshire-crop"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/environment/farming">Farming</category>
      <category domain="https://www.theguardian.com/uk-news/yorkshire">Yorkshire</category>
      <category domain="https://www.theguardian.com/food/british-food-and-drink">British food and drink</category>
      <category domain="https://www.theguardian.com/science/agriculture">Agriculture</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/food/vegetables">Vegetables</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/uk-news/north-of-england">North of England</category>
      <category domain="https://www.theguardian.com/uk/leeds">Leeds</category>
      <category domain="https://www.theguardian.com/uk-news/west-yorkshire">West Yorkshire</category>
      <pubDate>Sat, 01 Mar 2025 08:00:36 GMT</pubDate>
      <guid>https://www.theguardian.com/food/2025/mar/01/tickled-pink-rhubarb-growers-see-explosion-in-demand-for-yorkshire-crop</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b65cf23ac7757fc6333196be873ebaa745a8bdce/0_240_8192_4915/master/8192.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=63aa2183786c4592f901b549c0191801">
        <media:credit scheme="urn:ebu">Photograph: Christopher Thomond/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b65cf23ac7757fc6333196be873ebaa745a8bdce/0_240_8192_4915/master/8192.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d6c780809452a9602f5298635d79c8ee">
        <media:credit scheme="urn:ebu">Photograph: Christopher Thomond/The Guardian</media:credit>
      </media:content>
      <dc:creator>Robyn Vinter North of England correspondent</dc:creator>
      <dc:date>2025-03-01T08:00:36Z</dc:date>
    </item>
    <item>
      <title>Rail passengers in England and Wales face steep fares rise from Sunday</title>
      <link>https://www.theguardian.com/money/2025/mar/01/rail-passengers-england-wales-fares-rise</link>
      <description>&lt;p&gt;Campaigners say if government can find money to freeze fuel duty for motorists they can do similar for railways&lt;/p&gt;&lt;p&gt;Rail passengers in England and Wales face a steep increase in the cost of travel from Sunday, with fares rising by 4.6% and most railcards going up by £5.&lt;/p&gt;&lt;p&gt;The government said the rise is needed because of the dire financial state of the railway, but transport campaigners contrasted it with Labour prolonging the freeze on fuel duty for motorists.&lt;/p&gt; &lt;a href="https://www.theguardian.com/money/2025/mar/01/rail-passengers-england-wales-fares-rise"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/money/rail-fares">Rail fares</category>
      <category domain="https://www.theguardian.com/business/rail-industry">Rail industry</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <category domain="https://www.theguardian.com/uk/transport">Transport</category>
      <category domain="https://www.theguardian.com/uk/rail-transport">Rail transport</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/money/money">Money</category>
      <category domain="https://www.theguardian.com/money/consumer-affairs">Consumer affairs</category>
      <category domain="https://www.theguardian.com/uk-news/england">England</category>
      <category domain="https://www.theguardian.com/uk/wales">Wales</category>
      <pubDate>Sat, 01 Mar 2025 07:00:33 GMT</pubDate>
      <guid>https://www.theguardian.com/money/2025/mar/01/rail-passengers-england-wales-fares-rise</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/ca24deda725ce342d8b22abd099059e197c93f73/185_159_5269_3164/master/5269.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=46aec95e617c6e9c1bc0c169f4b7d946">
        <media:credit scheme="urn:ebu">Photograph: Geoffrey Swaine/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/ca24deda725ce342d8b22abd099059e197c93f73/185_159_5269_3164/master/5269.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=41a718d7b503e4bf1e3bfca16de58660">
        <media:credit scheme="urn:ebu">Photograph: Geoffrey Swaine/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Gwyn Topham Transport correspondent</dc:creator>
      <dc:date>2025-03-01T07:00:33Z</dc:date>
    </item>
    <item>
      <title>English academy chain to improve conditions for Jamaican teachers after strike threat</title>
      <link>https://www.theguardian.com/education/2025/mar/01/english-academy-chain-conditions-jamaican-teachers-neu</link>
      <description>&lt;p&gt;Union leader describes chain’s record on overseas-trained teachers as ‘Harris Federation’s Windrush’&lt;/p&gt;&lt;p&gt;The National Education Union has claimed a “resounding improvement” in workload and conditions for teachers from Jamaica and other countries at a leading academy chain, as part of a deal ending threats of strike action.&lt;/p&gt;&lt;p&gt;The Harris Federation of schools confirmed it will improve conditions for qualified teachers from Jamaica and others trained overseas, as part of a deal that eases the route for overseas-trained teachers to gain similar qualifications in England.&lt;/p&gt; &lt;a href="https://www.theguardian.com/education/2025/mar/01/english-academy-chain-conditions-jamaican-teachers-neu"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/education/teaching">Teaching</category>
      <category domain="https://www.theguardian.com/education/teacher-shortages">Teacher shortages</category>
      <category domain="https://www.theguardian.com/education/academies">Academies</category>
      <category domain="https://www.theguardian.com/politics/tradeunions">Trade unions</category>
      <category domain="https://www.theguardian.com/world/jamaica">Jamaica</category>
      <category domain="https://www.theguardian.com/education/teachertraining">Teacher training</category>
      <category domain="https://www.theguardian.com/education/schools">Schools</category>
      <category domain="https://www.theguardian.com/uk-news/industrial-action">Industrial action</category>
      <category domain="https://www.theguardian.com/uk-news/england">England</category>
      <category domain="https://www.theguardian.com/education/education">Education</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Sat, 01 Mar 2025 09:05:17 GMT</pubDate>
      <guid>https://www.theguardian.com/education/2025/mar/01/english-academy-chain-conditions-jamaican-teachers-neu</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3402c7eb42b941a202e280bcfa1c02a97cf098ef/0_62_3500_2100/master/3500.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a74dd2f77a88443520a60f14f47a8129">
        <media:credit scheme="urn:ebu">Photograph: David Jones/PA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3402c7eb42b941a202e280bcfa1c02a97cf098ef/0_62_3500_2100/master/3500.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=914087be42ece864ef77d0654cceebd1">
        <media:credit scheme="urn:ebu">Photograph: David Jones/PA</media:credit>
      </media:content>
      <dc:creator>Richard Adams Education editor</dc:creator>
      <dc:date>2025-03-01T09:05:17Z</dc:date>
    </item>
    <item>
      <title>Tortured death of trans man in western New York echoes notorious 90s killing</title>
      <link>https://www.theguardian.com/us-news/2025/mar/01/sam-nordquist-killing-matthew-shepard</link>
      <description>&lt;p&gt;Seven people have been arrested over ‘horrific’ death of Sam Nordquist, 24. A similar case in Wyoming once helped fuel the LGBTQ+ rights movement&lt;/p&gt;&lt;p&gt;A body discarded in a field. Cold weather. Signs of torture.&lt;/p&gt;&lt;p&gt;So far, seven people have been charged with the murder of Sam Nordquist, a 24-year-old Black transgender man who was tortured and murdered in western New York state last month. It was a case that Capt Kelly Swift of the state police’s bureau of criminal investigation said was “one of the most horrific crimes I have ever investigated”.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/mar/01/sam-nordquist-killing-matthew-shepard"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/us-crime">US crime</category>
      <category domain="https://www.theguardian.com/world/lgbt-rights">LGBTQ+ rights</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/us-news/new-york">New York</category>
      <category domain="https://www.theguardian.com/society/transgender">Transgender</category>
      <pubDate>Sat, 01 Mar 2025 16:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/mar/01/sam-nordquist-killing-matthew-shepard</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/f857e68f8711b685bae469988dc06d457c9f885c/0_0_5521_3312/master/5521.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7284bd2eec75a79296355abfb3d9ac6a">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/f857e68f8711b685bae469988dc06d457c9f885c/0_0_5521_3312/master/5521.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=519589a9f40b1d8093008fbb644c3172">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <dc:creator>Edward Helmore</dc:creator>
      <dc:date>2025-03-01T16:00:43Z</dc:date>
    </item>
    <item>
      <title>Pope Francis spends peaceful night after breathing crisis, Vatican says</title>
      <link>https://www.theguardian.com/world/2025/mar/01/pope-francis-spends-peaceful-night-after-breathing-crisis-vatican-says</link>
      <description>&lt;p&gt;Official says doctors caring for pontiff, 88, are assessing how Friday’s incident will affect his condition&lt;/p&gt;&lt;p&gt;Pope Francis, who has been in hospital for two weeks with pneumonia in both lungs, has spent a peaceful night after suffering a breathing crisis, the Vatican said.&lt;/p&gt;&lt;p&gt;Francis, 88, had suffered an “isolated breathing crisis” that caused him to vomit and provoked a “sudden worsening” of his respiratory condition, the Vatican said.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/pope-francis-spends-peaceful-night-after-breathing-crisis-vatican-says"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/pope-francis">Pope Francis</category>
      <category domain="https://www.theguardian.com/world/catholicism">Catholicism</category>
      <category domain="https://www.theguardian.com/world/christianity">Christianity</category>
      <category domain="https://www.theguardian.com/world/religion">Religion</category>
      <category domain="https://www.theguardian.com/world/the-papacy">The papacy</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/vatican">Vatican</category>
      <category domain="https://www.theguardian.com/world/italy">Italy</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <pubDate>Sat, 01 Mar 2025 09:17:35 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/pope-francis-spends-peaceful-night-after-breathing-crisis-vatican-says</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d42dc2c129f04fffadf21ba1c06c58e3310b5947/0_467_7000_4202/master/7000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=5fd047cc6103f762e7296adc549e93c8">
        <media:credit scheme="urn:ebu">Photograph: Alkis Konstantinidis/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d42dc2c129f04fffadf21ba1c06c58e3310b5947/0_467_7000_4202/master/7000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=601c2a340b19ef9d420d904293d0c457">
        <media:credit scheme="urn:ebu">Photograph: Alkis Konstantinidis/Reuters</media:credit>
      </media:content>
      <dc:creator>Guardian staff and agency</dc:creator>
      <dc:date>2025-03-01T09:17:35Z</dc:date>
    </item>
    <item>
      <title>Shortsighted Taiwan may have lessons for the world as a preventable disease skyrockets</title>
      <link>https://www.theguardian.com/world/2025/mar/01/shortsighted-taiwan-may-have-lessons-for-the-world-as-a-preventable-disease-skyrockets</link>
      <description>&lt;p&gt;Up to 90% of young people in Taiwan have myopia but eye experts say the growing global trend can be reversed&lt;/p&gt;&lt;p&gt;In the final days of their eight-week bootcamp, dozens of young Taiwanese conscripts are being tested on an obstacle course. The men in full combat kit are crawling underneath rows of razor wire and through bunkers as controlled explosions blast columns of dirt into the air. Pink and green smoke blooms in a simulated gas attack, requiring the conscripts to quickly don gas masks so they can rush the zone. But it’s here where many of them pause, stopping the assault drill to spend precious seconds removing their glasses so the masks will fit.&lt;/p&gt;&lt;p&gt;The conscripts mostly look to be in their early 20s. Statistics suggest that means anywhere up to 90% of them have some degree of myopia, otherwise known as shortsightedness.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/shortsighted-taiwan-may-have-lessons-for-the-world-as-a-preventable-disease-skyrockets"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/taiwan">Taiwan</category>
      <category domain="https://www.theguardian.com/world/asia-pacific">Asia Pacific</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/science/medical-research">Medical research</category>
      <pubDate>Fri, 28 Feb 2025 19:00:16 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/shortsighted-taiwan-may-have-lessons-for-the-world-as-a-preventable-disease-skyrockets</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/211e669ea0a78edd656e45c19fc52b2f323ac15f/778_0_1561_937/master/1561.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4e11a8987896cf192b05399c680fb4b0">
        <media:credit scheme="urn:ebu">Photograph: Helen Davidson/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/211e669ea0a78edd656e45c19fc52b2f323ac15f/778_0_1561_937/master/1561.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=11be3eaccaf310ca89974c15ef710e35">
        <media:credit scheme="urn:ebu">Photograph: Helen Davidson/The Guardian</media:credit>
      </media:content>
      <dc:creator>Helen Davidson in Taipei</dc:creator>
      <dc:date>2025-02-28T19:00:16Z</dc:date>
    </item>
    <item>
      <title>‘I want him to be prepared’: why parents are teaching their gen Alpha kids to use AI</title>
      <link>https://www.theguardian.com/technology/2025/mar/01/parents-children-artificial-intelligence</link>
      <description>&lt;p&gt;As AI grows increasingly prevalent, some are showing their children tools from ChatGPT to Dall-E to learn and bond&lt;/p&gt;&lt;p&gt;Jules White used to believe his 11-year-old son needed to know how to code to be successful. Now, though, the Vanderbilt computer science professor says it’s more crucial for James to learn a new, more useful skill: how to prompt &lt;a href="https://www.theguardian.com/technology/artificialintelligenceai"&gt;artificial intelligence&lt;/a&gt; (AI) &lt;a href="https://www.theguardian.com/technology/chatbots"&gt;chatbots&lt;/a&gt;.&lt;/p&gt;&lt;p&gt;Since &lt;a href="https://www.theguardian.com/technology/openai"&gt;OpenAI&lt;/a&gt; released &lt;a href="https://www.theguardian.com/technology/chatgpt"&gt;ChatGPT&lt;/a&gt; in 2022, White has been showing his son the ropes of generative AI. He began by demonstrating to James how ChatGPT can create games using photos of toys on the floor of their house. Later, White exposed him to AI’s hallucinatory flaws by having his son debunk ChatGPT-generated world record claims with verified information from the Guinness Book of World Records. After more than two years of experimentation, White’s son, now in fifth grade, has learned how to integrate AI into a range of everyday activities, from crafting study materials to determining the cost of shoes without a price tag.&lt;/p&gt; &lt;a href="https://www.theguardian.com/technology/2025/mar/01/parents-children-artificial-intelligence"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <category domain="https://www.theguardian.com/technology/artificialintelligenceai">Artificial intelligence (AI)</category>
      <category domain="https://www.theguardian.com/technology/chatgpt">ChatGPT</category>
      <category domain="https://www.theguardian.com/lifeandstyle/parents-and-parenting">Parents and parenting</category>
      <category domain="https://www.theguardian.com/lifeandstyle/family">Family</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/technology/computing">Computing</category>
      <pubDate>Sat, 01 Mar 2025 13:00:39 GMT</pubDate>
      <guid>https://www.theguardian.com/technology/2025/mar/01/parents-children-artificial-intelligence</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/0c0074c5028b5e4a8cc81330ada8f2edfd25d9e8/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3ed3efc22b3ba1b0f49c523be6a4c93c">
        <media:credit scheme="urn:ebu">Composite: The Guardian/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/0c0074c5028b5e4a8cc81330ada8f2edfd25d9e8/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2c85ce8c5c05519dd0b957f01e6c883f">
        <media:credit scheme="urn:ebu">Composite: The Guardian/Getty Images</media:credit>
      </media:content>
      <dc:creator>Aaron Mok</dc:creator>
      <dc:date>2025-03-01T13:00:39Z</dc:date>
    </item>
    <item>
      <title>‘She has this power’: nun’s crucifix links Michelangelo to Velázquez</title>
      <link>https://www.theguardian.com/world/2025/mar/01/she-has-this-power-nuns-crucifix-links-michelangelo-to-velazquez</link>
      <description>&lt;p&gt;Exclusive: Bronze cast of Christ connected to Florentine artist to be sold alongside Spanish masterpiece&lt;/p&gt;&lt;p&gt;A precociously talented artist, scarcely out of his teens, was in 1620 commissioned to paint the portrait of an intrepid nun passing through &lt;a href="https://www.theguardian.com/world/2019/may/25/seville-bid-to-save-velazquez-childhood-home-inspired-by-shakespeare"&gt;his home city of Seville&lt;/a&gt; on her way to one of the farthest outposts of Spain’s vast empire.&lt;/p&gt;&lt;p&gt;His painting reveals a shrewd, formidable woman in late middle age, who clasps a book in her left hand while wielding a crucifix, almost as if it were a weapon, in her right.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/mar/01/she-has-this-power-nuns-crucifix-links-michelangelo-to-velazquez"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/spain">Spain</category>
      <category domain="https://www.theguardian.com/artanddesign/diego-velazquez">Diego Velázquez</category>
      <category domain="https://www.theguardian.com/artanddesign/michelangelo">Michelangelo</category>
      <category domain="https://www.theguardian.com/artanddesign/art">Art</category>
      <category domain="https://www.theguardian.com/artanddesign/sculpture">Sculpture</category>
      <category domain="https://www.theguardian.com/world/religion">Religion</category>
      <category domain="https://www.theguardian.com/world/catholicism">Catholicism</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/christianity">Christianity</category>
      <pubDate>Sat, 01 Mar 2025 08:00:36 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/mar/01/she-has-this-power-nuns-crucifix-links-michelangelo-to-velazquez</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/2c5556d90d45f7bba670b8a58ba8c38f9e2ee303/300_687_3981_2388/master/3981.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d274ea4a1730e10de282b1b46153fa8f">
        <media:credit scheme="urn:ebu">Photograph: Handout</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/2c5556d90d45f7bba670b8a58ba8c38f9e2ee303/300_687_3981_2388/master/3981.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4001f13f499c54b487a35446315eea36">
        <media:credit scheme="urn:ebu">Photograph: Handout</media:credit>
      </media:content>
      <dc:creator>Sam Jones in Madrid</dc:creator>
      <dc:date>2025-03-01T08:00:36Z</dc:date>
    </item>
    <item>
      <title>‘Free world needs a new leader’, says EU foreign chief after Trump Zelenskyy row</title>
      <link>https://www.theguardian.com/world/2025/feb/28/european-leaders-throw-support-behind-zelenskyy-after-heated-trump-meeting</link>
      <description>&lt;p&gt;The EU foreign policy chief, Kaja Kallas, said ‘the free world needs a new leader’ and that it was up to Europeans to take this challenge&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Live reaction to Zelenskyy’s clash with Trump and Vance&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;The EU foreign policy chief has declared that “the free world needs a new leader”, as European leaders threw their support behind Ukraine’s president, Volodymyr Zelenskyy, after the stunning White House confrontation between him and Donald Trump.&lt;/p&gt;&lt;p&gt;Leaders from across Europe expressed their solidarity with the Ukrainian leader after the fractious exchange with JD Vance, the US vice-president, and Trump, who claimed he was not “ready for peace” &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-meeting-transcript"&gt;and accused him of “gambling with world war three”.&lt;/a&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/feb/28/european-leaders-throw-support-behind-zelenskyy-after-heated-trump-meeting"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/eu">European Union</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <pubDate>Fri, 28 Feb 2025 23:11:43 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/feb/28/european-leaders-throw-support-behind-zelenskyy-after-heated-trump-meeting</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/fc931d447525ceee0179a7be98c4c6526a4858c9/0_157_4723_2834/master/4723.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=7a99ecc19b4cd477fd42582e2096abe8">
        <media:credit scheme="urn:ebu">Photograph: Yves Herman/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/fc931d447525ceee0179a7be98c4c6526a4858c9/0_157_4723_2834/master/4723.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4ef6bcf06812bbbd142c5e52e2539fac">
        <media:credit scheme="urn:ebu">Photograph: Yves Herman/Reuters</media:credit>
      </media:content>
      <dc:creator>Nadeem Badshah</dc:creator>
      <dc:date>2025-02-28T23:11:43Z</dc:date>
    </item>
    <item>
      <title>Plane crashes have people freaked out – but here’s what US data for 2025 shows</title>
      <link>https://www.theguardian.com/us-news/2025/mar/01/plane-crash-safety-data</link>
      <description>&lt;p&gt;This year’s 16 fatal accidents fall below average rate of 20 per month, yet recent crashes were US’s deadliest in a decade&lt;/p&gt;&lt;p&gt;People across the US are worried about flight safety after several high-profile plane crashes this year, including a commercial crash in Washington DC that &lt;a href="https://www.theguardian.com/us-news/washington-dc-plane-crash"&gt;killed 67 people&lt;/a&gt;. Google searches for “is flying safe” &lt;a href="https://trends.google.com/trends/explore?date=today%205-y&amp;amp;geo=US&amp;amp;q=%22is%20flying%20safe%22&amp;amp;hl=en"&gt;have jumped&lt;/a&gt; in recent weeks. But the numbers suggest 2025 has actually been a relatively safe year to fly – at least in terms of the overall number of accidents.&lt;/p&gt;&lt;p&gt;January and February typically have about 20 fatal aviation accidents per month, according to &lt;a href="https://www.ntsb.gov/safety/data/Pages/monthly-dashboard.aspx"&gt;numbers&lt;/a&gt; from the National Transportation Safety Board. By contrast, this January, there were only 10 fatal aviation accidents, and in February there were six. The data covers all US civil aviation, from large commercial planes to private jets.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/mar/01/plane-crash-safety-data"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/plane-crashes">Plane crashes</category>
      <category domain="https://www.theguardian.com/world/air-transport">Air transport</category>
      <category domain="https://www.theguardian.com/us-news/washington-dc-plane-crash">Washington DC plane crash</category>
      <pubDate>Sat, 01 Mar 2025 15:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/mar/01/plane-crash-safety-data</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/e68d39213cf854ad1afaa3a345589c3bc91b7eca/0_0_3402_2042/master/3402.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b6f50497b606f1ab3b703192b2382106">
        <media:credit scheme="urn:ebu">Illustration: Mona Chalabi/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/e68d39213cf854ad1afaa3a345589c3bc91b7eca/0_0_3402_2042/master/3402.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e501dffc6667a7dbd3bbb7fcc6d871bf">
        <media:credit scheme="urn:ebu">Illustration: Mona Chalabi/The Guardian</media:credit>
      </media:content>
      <dc:creator>Mona Chalabi</dc:creator>
      <dc:date>2025-03-01T15:00:43Z</dc:date>
    </item>
    <item>
      <title>High heels and risky selfies: Etna eruptions cause despair among mountain rescuers</title>
      <link>https://www.theguardian.com/world/2025/feb/28/etna-eruptions-cause-despair-among-mountain-rescuers</link>
      <description>&lt;p&gt;Thousands of tourists arrived to see lava in recent weeks, but not all were prepared for treks up the Sicilian volcano&lt;/p&gt;&lt;p&gt;A river of fire from the depths of the Earth carves its way through the black rocks of a mountain blanketed white with snow. Above, the setting sun tints the clouds red. Fountains of lava that explode from a crater soar hundreds of metres into the air and Etna’s roar echoes across the Sicilian sky.&lt;/p&gt;&lt;p&gt;Its recent eruptions were a &lt;a href="https://www.theguardian.com/world/video/2025/feb/13/drone-footage-captures-breathtaking-mount-etna-eruption-video"&gt;breathtaking spectacle&lt;/a&gt;, drawing thousands of tourists and unwary daytrippers – many there for a selfie. For some, the outcome was catastrophic.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/feb/28/etna-eruptions-cause-despair-among-mountain-rescuers"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/italy">Italy</category>
      <category domain="https://www.theguardian.com/world/volcanoes">Volcanoes</category>
      <category domain="https://www.theguardian.com/world/mountaineering">Mountaineering</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/news/overtourism">Overtourism</category>
      <pubDate>Fri, 28 Feb 2025 12:00:11 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/feb/28/etna-eruptions-cause-despair-among-mountain-rescuers</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/be7fce9fd5aca11f3ce95cdc005bb10efd844e50/0_236_3543_2126/master/3543.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=977d994030ee5d723d48a971275eac37">
        <media:credit scheme="urn:ebu">Photograph: Antonio Parrinello/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/be7fce9fd5aca11f3ce95cdc005bb10efd844e50/0_236_3543_2126/master/3543.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=ec180003a854018ffc674968738b94dd">
        <media:credit scheme="urn:ebu">Photograph: Antonio Parrinello/The Guardian</media:credit>
      </media:content>
      <dc:creator>Lorenzo Tondo on Mount Etna. Photographs by Antonio Parrinello</dc:creator>
      <dc:date>2025-02-28T12:00:11Z</dc:date>
    </item>
    <item>
      <title>Rage in Greece as second anniversary of train disaster prompts mass protests</title>
      <link>https://www.theguardian.com/world/2025/feb/28/rage-in-greece-as-second-anniversary-of-train-disaster-prompts-mass-protests</link>
      <description>&lt;p&gt;Hundreds of thousands demonstrate amid outpouring of anger over state’s handling of Tempe tragedy&lt;/p&gt;&lt;p&gt;Two years to the day since 57 people died and dozens were injured in &lt;a href="https://www.theguardian.com/world/2023/mar/01/greece-train-crash-deaths-injuries-larissa-collision-derailment"&gt;Greece’s worst train crash in history&lt;/a&gt;, hundreds of thousands of protesters filled plazas around the country and a general strike paralysed the transport network in an outpouring of anger over the government’s handling of the tragedy.&lt;/p&gt;&lt;p&gt;By 11am on Friday, more than 100,000 people had already gathered in Syntagma Square in Athens. Thousands who could not get to the area due to packed metro trains instead vented their anger outside stations in the capital’s suburbs.&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/feb/28/rage-in-greece-as-second-anniversary-of-train-disaster-prompts-mass-protests"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/greece">Greece</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/train-crashes">Train crashes</category>
      <pubDate>Fri, 28 Feb 2025 18:47:51 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/feb/28/rage-in-greece-as-second-anniversary-of-train-disaster-prompts-mass-protests</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/a0b42cf7a4087c27ce5752176cccdc2e52e9a495/0_275_5500_3299/master/5500.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=f5b0d5a73abd99276e61a3e1dab0e469">
        <media:credit scheme="urn:ebu">Photograph: Petros Giannakouris/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/a0b42cf7a4087c27ce5752176cccdc2e52e9a495/0_275_5500_3299/master/5500.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2ba20d27f66052a9bd574d345c6c9ab9">
        <media:credit scheme="urn:ebu">Photograph: Petros Giannakouris/AP</media:credit>
      </media:content>
      <dc:creator>Helena Smith in Athens</dc:creator>
      <dc:date>2025-02-28T18:47:51Z</dc:date>
    </item>
    <item>
      <title>Meta apologises over flood of gore, violence and dead bodies on Instagram</title>
      <link>https://www.theguardian.com/technology/2025/feb/28/meta-apologises-over-flood-of-gore-violence-and-dead-bodies-on-instagram</link>
      <description>&lt;p&gt;Users of Reels report feeds dominated by violent and graphic footage after apparent algorithm malfunction&lt;/p&gt;&lt;p&gt;Mark Zuckerberg’s Meta has apologised after Instagram users were subjected to a flood of violence, gore, animal abuse and dead bodies on their Reels feeds.&lt;/p&gt;&lt;p&gt;Users reported the footage after an apparent malfunction in Instagram’s algorithm, which curates what people see on the app.&lt;/p&gt; &lt;a href="https://www.theguardian.com/technology/2025/feb/28/meta-apologises-over-flood-of-gore-violence-and-dead-bodies-on-instagram"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/technology/meta">Meta</category>
      <category domain="https://www.theguardian.com/technology/instagram">Instagram</category>
      <category domain="https://www.theguardian.com/media/social-media">Social media</category>
      <category domain="https://www.theguardian.com/media/digital-media">Digital media</category>
      <category domain="https://www.theguardian.com/media/media">Media</category>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <category domain="https://www.theguardian.com/technology/mark-zuckerberg">Mark Zuckerberg</category>
      <category domain="https://www.theguardian.com/technology/internet-safety">Internet safety</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Fri, 28 Feb 2025 15:01:04 GMT</pubDate>
      <guid>https://www.theguardian.com/technology/2025/feb/28/meta-apologises-over-flood-of-gore-violence-and-dead-bodies-on-instagram</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c9e51f4d6e838e3afbeffbbcacbbbf2152753ba3/54_1171_2277_1366/master/2277.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c2b943fcc268a569c993eddfe03da15e">
        <media:credit scheme="urn:ebu">Photograph: Alexey Panferov/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c9e51f4d6e838e3afbeffbbcacbbbf2152753ba3/54_1171_2277_1366/master/2277.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=930dc7dd170c0fdab3e9b91ecc76dc33">
        <media:credit scheme="urn:ebu">Photograph: Alexey Panferov/Alamy</media:credit>
      </media:content>
      <dc:creator>Dan Milmo Global technology editor</dc:creator>
      <dc:date>2025-02-28T15:01:04Z</dc:date>
    </item>
    <item>
      <title>The week in audio: Lucky Boy; Moorgate; Thirty Eulogies; Harford: An Oral History and more – review</title>
      <link>https://www.theguardian.com/tv-and-radio/2025/mar/01/the-week-in-audio-lucky-boy-moorgate-thirty-eulogies-harford-an-oral-history-and-more-review</link>
      <description>&lt;p&gt;Past traumas processed through investigative journalism and drama; a truly moving and surprising documentary; laugh-out-loud indie comedy; and Lauren Laverne’s return&lt;/p&gt;&lt;p&gt;&lt;strong&gt;&lt;a href="https://www.tortoisemedia.com/listen/lucky-boy"&gt;Lucky Boy&lt;/a&gt;&lt;/strong&gt; (Tortoise Media)&lt;br&gt;&lt;strong&gt;&lt;a href="https://www.bbc.co.uk/programmes/m0028bjc"&gt;Moorgate&lt;/a&gt;&lt;/strong&gt; (Radio 4/BBC Sounds)&lt;br&gt;&lt;strong&gt;&lt;a href="https://www.bbc.co.uk/programmes/m0028b77"&gt;Thirty Eulogies&lt;/a&gt;&lt;/strong&gt; (Radio 4/BBC Sounds)&lt;br&gt;&lt;strong&gt;&lt;a href="https://soundcloud.com/daniel-hooper-18/harford-s1e1-the-funeral"&gt;Harford: An Oral History&lt;/a&gt;&lt;/strong&gt; (Dan Hooper)&lt;br&gt;&lt;strong&gt;&lt;a href="https://www.bbc.co.uk/programmes/b00c000j"&gt;Lauren Laverne&lt;/a&gt;&lt;/strong&gt; (Radio 6 Music/BBC Sounds)&lt;/p&gt;&lt;p&gt;“In that summer, it was me and her against the world. We were powerful, right?” On Tortoise Media’s new four-part podcast, &lt;strong&gt;&lt;a href="https://www.tortoisemedia.com/listen/lucky-boy"&gt;Lucky Boy&lt;/a&gt;&lt;/strong&gt;, Gareth (not his real name) is remembering his first love. He was 14 then, bright but a “misfit”, having a secret relationship. She was 27 and a teacher. &lt;em&gt;Lucky Boy&lt;/em&gt; is how Gareth thought of himself at the time; nearly 40 years later, he thinks the opposite.&lt;/p&gt; &lt;a href="https://www.theguardian.com/tv-and-radio/2025/mar/01/the-week-in-audio-lucky-boy-moorgate-thirty-eulogies-harford-an-oral-history-and-more-review"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/tv-and-radio/podcasts">Podcasts</category>
      <category domain="https://www.theguardian.com/culture/radio">Radio</category>
      <category domain="https://www.theguardian.com/media/radio4">Radio 4</category>
      <category domain="https://www.theguardian.com/media/6music">6 Music</category>
      <category domain="https://www.theguardian.com/culture/comedy">Comedy</category>
      <category domain="https://www.theguardian.com/media/bbc">BBC</category>
      <category domain="https://www.theguardian.com/society/aids-and-hiv">Aids and HIV</category>
      <category domain="https://www.theguardian.com/tv-and-radio/lauren-laverne">Lauren Laverne</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/tv-and-radio/tv-and-radio">Television &amp; radio</category>
      <pubDate>Sat, 01 Mar 2025 17:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/tv-and-radio/2025/mar/01/the-week-in-audio-lucky-boy-moorgate-thirty-eulogies-harford-an-oral-history-and-more-review</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/cf53edc416dd6075bb5a6c03c36171fb61f1b83e/0_59_2007_1205/master/2007.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=cbcf8631794a17cd967d1d8cc6de6295">
        <media:credit scheme="urn:ebu">Photograph: PA</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/cf53edc416dd6075bb5a6c03c36171fb61f1b83e/0_59_2007_1205/master/2007.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4b5d3da46d6df5fd7ce6e4a568801152">
        <media:credit scheme="urn:ebu">Photograph: PA</media:credit>
      </media:content>
      <dc:creator>Jude Rogers</dc:creator>
      <dc:date>2025-03-01T17:00:44Z</dc:date>
    </item>
    <item>
      <title>James Bond nightclubs, vodka, aftershave: 007 writer on the spy’s future with Amazon</title>
      <link>https://www.theguardian.com/film/2025/mar/01/james-bond-william-boyd-spy-amazon-franchise-ai</link>
      <description>&lt;p&gt;As the Bond franchise heads to the online giant, thriller author William Boyd foresees a slew of spin-offs and says AI is not a threat to human screenwriters&lt;/p&gt;&lt;p&gt;Among the people best placed to predict how any James Bond of the future might look is a British writer with a strong feel for spies and for spying. &lt;a href="https://www.theguardian.com/books/2024/nov/01/william-boyd-fantasy-is-a-genre-that-i-cannot-abide-any-more"&gt;William Boyd&lt;/a&gt; has been drawn back to the terrain repeatedly in his books. What’s more, he wrote his own official &lt;a href="https://www.theguardian.com/books/2014/may/06/solo-james-bond-novel-william-boyd-ian-fleming-review"&gt;Bond novel, &lt;/a&gt;&lt;em&gt;&lt;a href="https://www.theguardian.com/books/2014/may/06/solo-james-bond-novel-william-boyd-ian-fleming-review"&gt;Solo&lt;/a&gt;&lt;/em&gt;&lt;a href="https://www.theguardian.com/books/2014/may/06/solo-james-bond-novel-william-boyd-ian-fleming-review"&gt;, in 2013&lt;/a&gt;.&lt;/p&gt;&lt;p&gt;Now Amazon has picked up the rights to the character, Boyd foresees a succession of 007 spin-off products and entertainments. Perhaps even be new AI-generated novels? “Certainly wait for Bond aftershave – and for the theme park and the dinner jackets,” he said. “The new owners will have to commodify everything about their billion-dollar purchase, so there will be nightclubs and vodkas.”&lt;/p&gt; &lt;a href="https://www.theguardian.com/film/2025/mar/01/james-bond-william-boyd-spy-amazon-franchise-ai"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/film/jamesbond">James Bond</category>
      <category domain="https://www.theguardian.com/books/william-boyd">William Boyd</category>
      <category domain="https://www.theguardian.com/books/ian-fleming">Ian Fleming</category>
      <category domain="https://www.theguardian.com/technology/amazon">Amazon</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/books/fiction">Fiction</category>
      <category domain="https://www.theguardian.com/books/thrillers">Thrillers</category>
      <category domain="https://www.theguardian.com/film/film">Film</category>
      <category domain="https://www.theguardian.com/film/film-industry">Film industry</category>
      <category domain="https://www.theguardian.com/technology/artificialintelligenceai">Artificial intelligence (AI)</category>
      <category domain="https://www.theguardian.com/books/books">Books</category>
      <category domain="https://www.theguardian.com/books/publishing">Publishing</category>
      <pubDate>Sat, 01 Mar 2025 13:27:14 GMT</pubDate>
      <guid>https://www.theguardian.com/film/2025/mar/01/james-bond-william-boyd-spy-amazon-franchise-ai</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/fa6c452d974ca838a50d3487b3ec5ad418b04eff/0_71_3000_1800/master/3000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=93f4ee085fd4c2ee952994330c80a847">
        <media:credit scheme="urn:ebu">Photograph: François Duhamel/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/fa6c452d974ca838a50d3487b3ec5ad418b04eff/0_71_3000_1800/master/3000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e9b15cd0766a4206bdf7912d3396d7f7">
        <media:credit scheme="urn:ebu">Photograph: François Duhamel/AP</media:credit>
      </media:content>
      <dc:creator>Vanessa Thorpe</dc:creator>
      <dc:date>2025-03-01T13:27:14Z</dc:date>
    </item>
    <item>
      <title>Behind the curtain: what really goes on in theatre dressing rooms?</title>
      <link>https://www.theguardian.com/culture/2025/mar/01/theatre-backstage-lithgow-coogan-paapa-denise-gough-vanessa-williams-erin-doherty</link>
      <description>&lt;p&gt;Ahead of next month’s Olivier awards, photographer David Levene reveals the secrets of life backstage in London’s West End, capturing the likes of Steve Coogan, Vanessa Williams, Paapa Essiedu and John Lithgow as they prepare for performance&lt;/p&gt;&lt;p&gt;Lightbulb-wreathed mirrors, wigs and makeup artists, a sense of faded glamour: the backstage dressing room has its very own lore in Theatreland. It is a private space for a company of actors to gear up or wind down, in between slipping into character, but it’s so much more than that. Films such as All About Eve and John Cassavetes’s Opening Night, as well as plays such as Ronald Harwood’s The Dresser, show this space bristling with tension, vulnerability and rivalries. And Judi Dench has spoken about the fun to be had in this other, unseen side of the proscenium arch (&lt;a href="https://www.standard.co.uk/culture/tvfilm/kenneth-branagh-tells-graham-norton-judi-dench-flashed-me-in-a-dressing-room-a3675936.html"&gt;including accidentally flashing Kenneth Branagh&lt;/a&gt;).&lt;/p&gt;&lt;p&gt;Denise Gough (centre), who plays Emma in People, Places and Things, chats with her fellow Emmas during the interval at the Trafalgar Studios theatre.&lt;/p&gt; &lt;a href="https://www.theguardian.com/culture/2025/mar/01/theatre-backstage-lithgow-coogan-paapa-denise-gough-vanessa-williams-erin-doherty"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/stage/theatre">Theatre</category>
      <pubDate>Sat, 01 Mar 2025 11:55:37 GMT</pubDate>
      <guid>https://www.theguardian.com/culture/2025/mar/01/theatre-backstage-lithgow-coogan-paapa-denise-gough-vanessa-williams-erin-doherty</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/65cd5d69a2eb6f7f22456dd858f10f1d2613f7cf/0_1434_7450_4472/master/7450.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=8f78c9245066b0889afaedcffe4f5ed1">
        <media:credit scheme="urn:ebu">Photograph: David Levene/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/65cd5d69a2eb6f7f22456dd858f10f1d2613f7cf/0_1434_7450_4472/master/7450.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=69e2858e22d8f2349555a7abb96193b6">
        <media:credit scheme="urn:ebu">Photograph: David Levene/The Guardian</media:credit>
      </media:content>
      <dc:creator>David Levene ; Introduction by Arifa Akbar</dc:creator>
      <dc:date>2025-03-01T11:55:37Z</dc:date>
    </item>
    <item>
      <title>‘Books picked me up on bad days’: how reading romance helped Lucy Mangan through grief</title>
      <link>https://www.theguardian.com/books/2025/mar/01/books-picked-me-up-on-bad-days-how-reading-romance-helped-lucy-mangan-through-grief</link>
      <description>&lt;p&gt;After the death of her father, the writer took refuge in the kinds of stories she had once written off – discovering a comforting world of funny heroines and happy endings&lt;/p&gt;&lt;p&gt;Grief is an intensifier. It&amp;nbsp;doesn’t often – despite what films and television would have you believe – cause you&amp;nbsp;to act massively out&amp;nbsp;of character. Like motherhood or any other huge life upheaval, its actual effect is to strip away the nonsense and leave your essential nature, your&amp;nbsp;core, not just intact but now unobscured by everyday concerns and&amp;nbsp;frivolities.&lt;/p&gt;&lt;p&gt;So it was no real surprise to find myself, in the immediate weeks after the death of my beloved dad in 2023, flinging myself into books. I would have done so literally, if I could. I wanted to gather my physical books into a wall – or better yet, a cave – around me that would both protect me from this new reality and let me cry in peace within it. Failing that, I took mental refuge in them instead.&lt;/p&gt; &lt;a href="https://www.theguardian.com/books/2025/mar/01/books-picked-me-up-on-bad-days-how-reading-romance-helped-lucy-mangan-through-grief"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/books/fiction">Fiction</category>
      <category domain="https://www.theguardian.com/books/romance">Romance books</category>
      <category domain="https://www.theguardian.com/books/books">Books</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/lifeandstyle/bereavement">Bereavement</category>
      <category domain="https://www.theguardian.com/lifeandstyle/parents-and-parenting">Parents and parenting</category>
      <category domain="https://www.theguardian.com/lifeandstyle/family">Family</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 11:00:36 GMT</pubDate>
      <guid>https://www.theguardian.com/books/2025/mar/01/books-picked-me-up-on-bad-days-how-reading-romance-helped-lucy-mangan-through-grief</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/895711b0e0e5e4d7a24af8ea64b80d7b7bb1c8a2/0_712_8880_5328/master/8880.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4df99846ff6e00c9280fbbfc1fb28ff7">
        <media:credit scheme="urn:ebu">Photograph: Amit Lennon/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/895711b0e0e5e4d7a24af8ea64b80d7b7bb1c8a2/0_712_8880_5328/master/8880.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=02c9f22037442628354ded8d721e78de">
        <media:credit scheme="urn:ebu">Photograph: Amit Lennon/The Guardian</media:credit>
      </media:content>
      <dc:creator>Lucy Mangan</dc:creator>
      <dc:date>2025-03-01T11:00:36Z</dc:date>
    </item>
    <item>
      <title>‘I don’t know whether I’d describe it as fun’: Aimee Lou Wood on the intensity of making The White Lotus</title>
      <link>https://www.theguardian.com/culture/2025/mar/01/i-dont-know-whether-id-describe-it-as-fun-aimee-lou-wood-on-the-intensity-of-making-the-white-lotus</link>
      <description>&lt;p&gt;She is the Sex Education star now stealing the show in Mike White’s hit series and about to appear in a gritty new Netflix drama. It’s all she ever wanted – but somehow, this ‘sad and shy’ actor finds folding the washing more rewarding than fame&lt;/p&gt;&lt;p&gt;Aimee Lou Wood has a peculiar habit of losing herself. She is known among her fellow&amp;nbsp;cast&amp;nbsp;members&amp;nbsp;for&amp;nbsp;capsizing so completely into a role that they can’t tell who they’re talking to: Wood or her character. (This probably wasn’t helped by the fact that her standout debut role in &lt;a href="https://www.theguardian.com/tv-and-radio/2019/jan/11/sex-education-review-netflix-asa-butterfield-gillian-anderson"&gt;Sex Education&lt;/a&gt; was also called Aimee.) &lt;a href="https://www.imdb.com/name/nm1013087/?ref_=nv_sr_srsg_0_tt_3_nm_5_in_0_q_suranne%2520jones"&gt;Suranne Jones&lt;/a&gt;, who plays alongside her in forthcoming dramedy Film Club, even bought her a bag with a big A on it: “And she said to me,” Wood says, “‘You can put things in there, and that’s Aimee’s bag, so you don’t lose who you are.’ My imagination and my reality can get scarily blurred.”&lt;/p&gt;&lt;p&gt;Wood has been searching for more clarity recently. “I’ve noticed more and more that I’m thinking: what do I actually want? Where can I be the driver and not the passenger?” I meet the 30-year-old in a kind of yoga-adjacent cafe in London. She’s got a &lt;a href="https://en.wikipedia.org/wiki/Shelley_Duvall"&gt;Shelley Duvall &lt;/a&gt;thing going on, where you can’t tell whether her face – wide open eyes like a Disney fawn, tentative smile – is what makes her seem honest yet mysterious, or whether those qualities created her face. Either way, she looks both very film star, in leather blazer, Dr Martens and miniskirt, yet also not out of place in this hippyish restaurant.&lt;/p&gt; &lt;a href="https://www.theguardian.com/culture/2025/mar/01/i-dont-know-whether-id-describe-it-as-fun-aimee-lou-wood-on-the-intensity-of-making-the-white-lotus"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/culture/aimee-lou-wood">Aimee Lou Wood</category>
      <category domain="https://www.theguardian.com/culture/television">Television</category>
      <category domain="https://www.theguardian.com/film/drama">Drama films</category>
      <category domain="https://www.theguardian.com/film/film">Film</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/tv-and-radio/sex-education">Sex Education</category>
      <category domain="https://www.theguardian.com/tv-and-radio/the-white-lotus">The White Lotus</category>
      <pubDate>Sat, 01 Mar 2025 07:00:34 GMT</pubDate>
      <guid>https://www.theguardian.com/culture/2025/mar/01/i-dont-know-whether-id-describe-it-as-fun-aimee-lou-wood-on-the-intensity-of-making-the-white-lotus</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d74ea0dea3607075789fbe55633aeef686b79eea/0_1758_6192_3715/master/6192.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0aa32e9ae7700a97343062a6b927b736">
        <media:credit scheme="urn:ebu">Photograph: Hollie Fernando/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d74ea0dea3607075789fbe55633aeef686b79eea/0_1758_6192_3715/master/6192.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3f1fc3e1b9a6620e2ffed762b3b3b9ec">
        <media:credit scheme="urn:ebu">Photograph: Hollie Fernando/The Guardian</media:credit>
      </media:content>
      <dc:creator>Zoe Williams</dc:creator>
      <dc:date>2025-03-01T07:00:34Z</dc:date>
    </item>
    <item>
      <title>TV tonight: it’s time to meet your new favourite detective!</title>
      <link>https://www.theguardian.com/tv-and-radio/2025/mar/01/tv-tonight-time-to-meet-your-new-favourite-detective-the-one-that-got-away-bbc</link>
      <description>&lt;p&gt;Welsh crime drama The One That Got Away is an addictive watch with a hotshot lead. Plus, Sabrina Carpenter will dazzle the stage at the 2025 Brits. Here’s what to watch this evening&lt;/p&gt;&lt;p&gt;&lt;strong&gt;9pm, BBC Four&lt;br&gt;&lt;/strong&gt;This knotty Welsh crime drama opens with a nurse heading for a romantic weekend in Paris. She is later found dead in the woods, with a heart-knot carved into a nearby tree. Enter your new favourite no-nonsense detective: Ffion Lloyd (Elen Rhys). The hotshot is called back from Cardiff to team up with ex-partner (and lover!) DS Rick Sheldon (Richard Harrington), and the pair wonder if, based on a previous murder they solved, there is a copycat killer on the loose. &lt;em&gt;Hollie Richardson&lt;/em&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/tv-and-radio/2025/mar/01/tv-tonight-time-to-meet-your-new-favourite-detective-the-one-that-got-away-bbc"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/tv-and-radio/tv-and-radio">Television &amp; radio</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/culture/television">Television</category>
      <pubDate>Sat, 01 Mar 2025 06:20:30 GMT</pubDate>
      <guid>https://www.theguardian.com/tv-and-radio/2025/mar/01/tv-tonight-time-to-meet-your-new-favourite-detective-the-one-that-got-away-bbc</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/11a00576bc67e7e6d3de39a81ea84470908120a7/0_55_4284_2570/master/4284.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d2bfb71bb811ce91e6feb1bd67ed6324">
        <media:credit scheme="urn:ebu">Photograph: Simon Ridgway/BBC/Backlight</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/11a00576bc67e7e6d3de39a81ea84470908120a7/0_55_4284_2570/master/4284.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=58418b93b3f2c77a25afbed8959778a6">
        <media:credit scheme="urn:ebu">Photograph: Simon Ridgway/BBC/Backlight</media:credit>
      </media:content>
      <dc:creator>Hollie Richardson, Ali Catterall, Hannah J Davies and Simon Wardell</dc:creator>
      <dc:date>2025-03-01T06:20:30Z</dc:date>
    </item>
    <item>
      <title>Cappuccino nails, boho blouses and pilates pumps: Jess Cartner-Morley’s March style essentials</title>
      <link>https://www.theguardian.com/thefilter/2025/mar/01/march-style-essentials</link>
      <description>&lt;p&gt;Are check prints the new floral? Are brooches really back? Our fashion guru has all the answers in her March edit&lt;/p&gt;&lt;p&gt;• &lt;strong&gt;&lt;a href="https://www.theguardian.com/thefilter/2025/feb/26/stylish-womens-raincoats"&gt;From Scandi brands to plastic-free fabrics: 10 women’s raincoats to style out drizzly days&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;We think before we buy these days, right? And so we damn well should. Shopping should not be a pastime. Style is eternal, not disposable. Each piece that has made the cut into my edit of March treasures has earned its place. A pair of £10 socks that are an instant wardrobe update. A £39 blazer that would pass for a designer investment. The ideal new-season nail polish for £8.&lt;/p&gt;&lt;p&gt;And you don’t have to buy anything at all for it to be worth having a look at my list. See below for my thoughts on what makes a boho blouse cool rather than twee, and a tipoff about the return of the brooch. That’s what I love about fashion: you don’t have to buy it to buy into it. But whatever you do, don’t buy anything until you read this.&lt;/p&gt; &lt;a href="https://www.theguardian.com/thefilter/2025/mar/01/march-style-essentials"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/fashion/fashion">Fashion</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/fashion/accessories">Accessories</category>
      <category domain="https://www.theguardian.com/fashion/womens-shoes">Women's shoes</category>
      <category domain="https://www.theguardian.com/fashion/women-s-shirts">Women's shirts</category>
      <category domain="https://www.theguardian.com/fashion/beauty">Beauty</category>
      <pubDate>Sat, 01 Mar 2025 15:00:42 GMT</pubDate>
      <guid>https://www.theguardian.com/thefilter/2025/mar/01/march-style-essentials</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/39c6144636430e289f555de07c04a703105935e7/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=8bed1b2a56bc2a492c734ef149bb4b27">
        <media:credit scheme="urn:ebu">Composite: PR Image</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/39c6144636430e289f555de07c04a703105935e7/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3358443e7bb2cfb26dd690c5642e3a73">
        <media:credit scheme="urn:ebu">Composite: PR Image</media:credit>
      </media:content>
      <dc:creator>Jess Cartner-Morley</dc:creator>
      <dc:date>2025-03-01T15:00:42Z</dc:date>
    </item>
    <item>
      <title>Want to avoid forever chemicals? Here are nine of the best PFAS-free frying pans</title>
      <link>https://www.theguardian.com/thefilter/2025/feb/28/best-pfas-free-frying-pans</link>
      <description>&lt;p&gt;Reducing forever chemicals in your kitchen is difficult, but possible. To help you start, we’ve rounded up the top non-toxic pans for Pancake Day and beyond&lt;/p&gt;&lt;p&gt;• &lt;strong&gt;&lt;a href="https://www.theguardian.com/food/article/2024/jul/10/the-best-kitchen-knives-for-every-job-chosen-by-chefs"&gt;The best kitchen knives for every job – chosen by chefs&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;Whether you’re making pancakes, seared steaks or fluffy omelettes, a frying pan that sizzles food without sticking to it is a kitchen necessity. Yet health and environmental concerns about non-stick coatings and “forever chemicals” are making it increasingly complicated to pick the perfect pan.&lt;/p&gt;&lt;p&gt;Manufacturers of non-stick coatings insist they’re perfectly safe, but a growing number of companies are advertising their products as PFOA- or PFOS-free, all the same. So what are these controversial chemicals, and what’s the alternative if you don’t want your food to stick?&lt;/p&gt; &lt;a href="https://www.theguardian.com/thefilter/2025/feb/28/best-pfas-free-frying-pans"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/environment/pfas">PFAS</category>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/lifeandstyle/health-and-wellbeing">Health &amp; wellbeing</category>
      <pubDate>Fri, 28 Feb 2025 15:05:27 GMT</pubDate>
      <guid>https://www.theguardian.com/thefilter/2025/feb/28/best-pfas-free-frying-pans</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5b730850c2d2e3ff6321962d1ba88ba624b4c0d6/0_155_6000_3600/master/6000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9a11ecaf845b6ad226172a8a9938c074">
        <media:credit scheme="urn:ebu">Photograph: xalanx/Getty Images/iStockphoto</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5b730850c2d2e3ff6321962d1ba88ba624b4c0d6/0_155_6000_3600/master/6000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=24ab46894758494cacf5bf4c6d98770d">
        <media:credit scheme="urn:ebu">Photograph: xalanx/Getty Images/iStockphoto</media:credit>
      </media:content>
      <dc:creator>Linda Geddes</dc:creator>
      <dc:date>2025-02-28T15:05:27Z</dc:date>
    </item>
    <item>
      <title>‘My house filled with stuff while my bank account drained’: how I stopped impulse buying</title>
      <link>https://www.theguardian.com/thefilter/2025/feb/27/how-to-stop-impulse-buying</link>
      <description>&lt;p&gt;How to quit the ‘buy now’ habit in eight easy steps – and shop smarter with forever in mind&lt;/p&gt;&lt;p&gt;• &lt;strong&gt;&lt;a href="https://www.theguardian.com/thefilter/2024/oct/20/if-you-pay-more-than-4-youre-being-ripped-off-the-fair-price-for-14-everyday-items-from-cleaning-spray-to-olive-oil"&gt;‘If you pay more than £4, you’re being ripped off’: the fair price for 14 everyday items&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;Introversion is rarely useful, but it saved me a fortune in my younger years. So keenly did I loathe going to the shops that I just didn’t spend much money. I was perfectly happy, albeit a little bored and usually dressed in the same clothes.&lt;/p&gt;&lt;p&gt;Then online shopping happened. The lure of one-click, next-day consumables unleashed my inner impulse buyer like a starving castaway at a buffet. I never quite became a shopping addict, but the thrill of home delivery fuelled a period of slightly unhinged affluenza. My house filled with stuff while my bank account drained. I accumulated retro camera kit (70% unused to this day), expensive books about using said camera kit (100% unread) and an untold number of dresses that I bought only because I could send them back for free. I never did send them back, of course, and I never wore them, because I never wear dresses. But they were so pretty.&lt;/p&gt; &lt;a href="https://www.theguardian.com/thefilter/2025/feb/27/how-to-stop-impulse-buying"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/fashion/fashion">Fashion</category>
      <pubDate>Thu, 27 Feb 2025 15:00:29 GMT</pubDate>
      <guid>https://www.theguardian.com/thefilter/2025/feb/27/how-to-stop-impulse-buying</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/694d9aeceb46f8e8e63caaa44611d2c8bc411ccb/0_567_8500_5100/master/8500.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=50cf0ca84be4fbaab65b52776c72e155">
        <media:credit scheme="urn:ebu">Photograph: Oscar Wong/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/694d9aeceb46f8e8e63caaa44611d2c8bc411ccb/0_567_8500_5100/master/8500.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=cdbaa4642b82ad0944425e9d4ce6c57b">
        <media:credit scheme="urn:ebu">Photograph: Oscar Wong/Getty Images</media:credit>
      </media:content>
      <dc:creator>Jane Hoskyn</dc:creator>
      <dc:date>2025-02-27T15:00:29Z</dc:date>
    </item>
    <item>
      <title>From Scandi brands to plastic-free fabrics: 10 women’s raincoats to style out drizzly days</title>
      <link>https://www.theguardian.com/thefilter/2025/feb/26/stylish-womens-raincoats</link>
      <description>&lt;p&gt;Don’t let spring showers ruin your look. Refresh your outerwear with these fashionable yet functional favourites&lt;/p&gt;&lt;p&gt;• &lt;strong&gt;&lt;a href="https://www.theguardian.com/thefilter/2024/dec/15/best-womens-waterproof-jackets"&gt;The best women’s waterproof jackets for every type of adventure, reviewed and rated&lt;/a&gt;&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;We’ve all heard the adage, “There’s no such thing as bad weather, only bad clothes.” I’ve always felt this obfuscates the fact that winter can be tough precisely because of its soggy, grey – yes, “bad” – weather, even if you’re dressed head to toe in warm waterproofs. However, I now have to admit that the right clothes for the right weather can bring a freedom. Enter: the humble anorak.&lt;/p&gt;&lt;p&gt;I spent my 20s dreading winters, which I spent freezing, only thawing out come summer. Even then, a light July sprinkling might have stopped me in my tracks, so opposed was I to clothes that made concessions for meteorological conditions. But with adulthood came an admission that anoraks are low-key amazing. I bought one and – by keeping me dry and therefore warmer – it has, without hyperbole, changed my life.&lt;/p&gt; &lt;a href="https://www.theguardian.com/thefilter/2025/feb/26/stylish-womens-raincoats"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/fashion/fashion">Fashion</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/fashion/womens-coats">Women's coats and jackets</category>
      <pubDate>Wed, 26 Feb 2025 15:00:38 GMT</pubDate>
      <guid>https://www.theguardian.com/thefilter/2025/feb/26/stylish-womens-raincoats</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3133dfad2cb64255e1670d99134915ffc3943454/0_720_4800_2880/master/4800.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=01a759538c6422f2f6e8f57a2a47c0aa">
        <media:credit scheme="urn:ebu">Photograph: Eshma/Getty Images/iStockphoto</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3133dfad2cb64255e1670d99134915ffc3943454/0_720_4800_2880/master/4800.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b24a7dd202bad257cca97f77d6b9c7f2">
        <media:credit scheme="urn:ebu">Photograph: Eshma/Getty Images/iStockphoto</media:credit>
      </media:content>
      <dc:creator>Ellie Violet Bramley</dc:creator>
      <dc:date>2025-02-26T15:00:38Z</dc:date>
    </item>
    <item>
      <title>Blood, sweat, tears and body shaming: a cartoonist’s guide to becoming a mother</title>
      <link>https://www.theguardian.com/lifeandstyle/picture/2025/mar/01/blood-sweat-tears-and-body-shaming-a-cartoonists-guide-to-becoming-a-mother</link>
      <description>&lt;p&gt;A comic artist illustrates the screamy horror of first-time parenthood&lt;/p&gt;&lt;p&gt;• &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/becky-barnicoat-on-turning-motherhood-into-cartoons"&gt;Read Becky Barnicoat on turning motherhood into cartoons&lt;/a&gt;&lt;/p&gt;&lt;p&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/picture/2025/mar/01/blood-sweat-tears-and-body-shaming-a-cartoonists-guide-to-becoming-a-mother"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/parents-and-parenting">Parents and parenting</category>
      <category domain="https://www.theguardian.com/lifeandstyle/family">Family</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/lifeandstyle/childbirth">Childbirth</category>
      <category domain="https://www.theguardian.com/lifeandstyle/health-and-wellbeing">Health &amp; wellbeing</category>
      <category domain="https://www.theguardian.com/books/books">Books</category>
      <pubDate>Sat, 01 Mar 2025 11:00:37 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/picture/2025/mar/01/blood-sweat-tears-and-body-shaming-a-cartoonists-guide-to-becoming-a-mother</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/1307bc9917979840a46f48b52a96c95cd701554d/221_2299_3608_2163/master/3608.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c5136df5de061215ac2146cd3b60218b">
        <media:credit scheme="urn:ebu">Illustration: Becky Barnicoat</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/1307bc9917979840a46f48b52a96c95cd701554d/221_2299_3608_2163/master/3608.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=deabe3a6d3060967480e870b4a7245d0">
        <media:credit scheme="urn:ebu">Illustration: Becky Barnicoat</media:credit>
      </media:content>
      <dc:creator>Becky Barnicoat</dc:creator>
      <dc:date>2025-03-01T11:00:37Z</dc:date>
    </item>
    <item>
      <title>Paul Ready: ‘ I wanted to be a tennis player – but I was crap’</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/paul-ready-i-wanted-to-be-a-tennis-player-but-i-was-crap</link>
      <description>&lt;p&gt;The actor, 48, on stage fright, dashed tennis dreams, meeting a naked Harvey Keitel and why he loves Mortherland&lt;/p&gt;&lt;p&gt;&lt;strong&gt;When I was five,&lt;/strong&gt; I was in the front row of the choir for our school nativity and couldn’t hack it. I pulled a sickie, started crying, had to get out. I don’t know why the hell I wanted to perform in front of people.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;My parents’ mantra &lt;/strong&gt;was “do what makes you happy if you can”. They didn’t push my three siblings and me to be academic. Our home was beautifully relaxed. Behaving well and being nice was drilled into us. And we could throw a party and they would stay out of the way.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/paul-ready-i-wanted-to-be-a-tennis-player-but-i-was-crap"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/stage/acting">Acting</category>
      <category domain="https://www.theguardian.com/culture/television">Television</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <pubDate>Sat, 01 Mar 2025 14:00:41 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/paul-ready-i-wanted-to-be-a-tennis-player-but-i-was-crap</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c4efe6600c9a4d42154c51f2c0ff48d7dd51ee12/0_932_4350_2609/master/4350.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=601950dd2de7895e88b4cb0a5cd77222">
        <media:credit scheme="urn:ebu">Photograph: Sarah Noons</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c4efe6600c9a4d42154c51f2c0ff48d7dd51ee12/0_932_4350_2609/master/4350.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=366a555723cbf7d5780cb5c74a1deffc">
        <media:credit scheme="urn:ebu">Photograph: Sarah Noons</media:credit>
      </media:content>
      <dc:creator>Hayley Myers</dc:creator>
      <dc:date>2025-03-01T14:00:41Z</dc:date>
    </item>
    <item>
      <title>Blind date: ‘I was quite vocal about my distaste for German food – and then learned of her German heritage’</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/blind-date-yukari-john</link>
      <description>&lt;p&gt;Yukari, 29, a freelance motion designer, meets John, 31, a furniture sales manager&lt;/p&gt;&lt;p&gt;&lt;strong&gt;What were you hoping for?&lt;/strong&gt;&lt;br&gt;
 Dating in a big city can be such a slog, so I was really just hoping for a connection. It’s always exciting to be set up on a date in a different way.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/blind-date-yukari-john"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/relationships">Relationships</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 06:00:31 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/blind-date-yukari-john</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/42d4866022b62694f252c31ca76cc4822f4abc4e/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=37234ad7f4f84000dfb7cb30daf4b3a0">
        <media:credit scheme="urn:ebu">Composite: Jill Mead &amp; Graeme Robertson/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/42d4866022b62694f252c31ca76cc4822f4abc4e/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=344c1f8c9e3885f2a6988a58601af378">
        <media:credit scheme="urn:ebu">Composite: Jill Mead &amp; Graeme Robertson/The Guardian</media:credit>
      </media:content>
      <dc:creator>Guardian Staff</dc:creator>
      <dc:date>2025-03-01T06:00:31Z</dc:date>
    </item>
    <item>
      <title>Justin Hawkins: ‘The worst thing anyone’s said to me? “I would love to go on a date with you, but I’d be too embarrassed”’ | The Q&amp;A</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/justin-hawkins-the-worst-thing-anyones-said-to-me-i-would-love-to-go-on-a-date-with-you-but-id-be-too-embarrassed</link>
      <description>&lt;p&gt;The Darkness frontman on intrusive thoughts, stamping on cockroaches and a terrifying flight to South Africa&lt;/p&gt;&lt;p&gt;Born in Surrey, Justin Hawkins, 49, wrote music for adverts and played in bands with his brother, Dan. In 2000 they formed the Darkness; their hit singles include I Believe in a&amp;nbsp;Thing Called Love, and they won three Brits and an Ivor Novello before splitting in 2006. Having reformed in 2011, the Darkness toured with Lady Gaga and their 2017 record, Pinewood Smile, became their third UK Top 10 album. On 6 March they start a UK tour and their new album is Dreams on Toast. Justin lives in Switzerland.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;What is your greatest fear?&lt;/strong&gt;&lt;br&gt;
 Fear is for frightened people. I’m not one of those.&lt;br&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/justin-hawkins-the-worst-thing-anyones-said-to-me-i-would-love-to-go-on-a-date-with-you-but-id-be-too-embarrassed"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/music/the-darkness">The Darkness</category>
      <category domain="https://www.theguardian.com/music/popandrock">Pop and rock</category>
      <category domain="https://www.theguardian.com/music/music">Music</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Sat, 01 Mar 2025 09:30:34 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/justin-hawkins-the-worst-thing-anyones-said-to-me-i-would-love-to-go-on-a-date-with-you-but-id-be-too-embarrassed</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/a215deaf65e0de95086536f9ceabb15cd5a68310/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=8313ae5ab9615eb533579d9cfcc323d7">
        <media:credit scheme="urn:ebu">Photograph: Ken McKay/ITV/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/a215deaf65e0de95086536f9ceabb15cd5a68310/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=cd4bb2d758d996a945ad7994c637937b">
        <media:credit scheme="urn:ebu">Photograph: Ken McKay/ITV/Shutterstock</media:credit>
      </media:content>
      <dc:creator>Rosanna Greenstreet</dc:creator>
      <dc:date>2025-03-01T09:30:34Z</dc:date>
    </item>
    <item>
      <title>Nord, Liverpool: ‘It’s very much a win’ - restaurant review</title>
      <link>https://www.theguardian.com/food/2025/mar/01/nord-liverpool-its-very-much-a-win-restaurant-review</link>
      <description>&lt;p&gt;If chefs were footballers, Nord’s Daniel Heffy would be in a league of his own&lt;/p&gt;&lt;p&gt;&lt;strong&gt;&lt;a href="http://nordrestaurant.co.uk/"&gt;Nord&lt;/a&gt;, 100 Old Hall St, Liverpool L3 9QJ. Snacks £6.50-£11; small plates £15.50-£27, large plates £20-£36&lt;/strong&gt;&lt;strong&gt;, desserts £11-£16&lt;/strong&gt;&lt;strong&gt;, wines from £32&lt;/strong&gt;&lt;/p&gt;&lt;p&gt;A midweek night and the restaurant is completely empty. Music thrums and staff drift about looking purposeful, despite being a little short on purpose until we show up. This has nothing to do with Nord and everything to do with football. At the exact time of our booking, Everton are kicking off against Liverpool, two miles away at Goodison Park, for what has been described to me as not just a game, but &lt;em&gt;the&lt;/em&gt; game. As well as being a local derby, it’s also the last ever match to be played between the two at the stadium before Everton move to their new home at &lt;a href="https://www.evertonstadium.com/"&gt;Bramley-Moore Dock&lt;/a&gt;. Even a blithering football ignoramus like me can recognise the significance of such a game to a city like Liverpool and why that might suppress bookings.&lt;/p&gt; &lt;a href="https://www.theguardian.com/food/2025/mar/01/nord-liverpool-its-very-much-a-win-restaurant-review"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/food/restaurants">Restaurants</category>
      <category domain="https://www.theguardian.com/travel/restaurants">Restaurants</category>
      <category domain="https://www.theguardian.com/travel/travel">Travel</category>
      <pubDate>Sat, 01 Mar 2025 06:00:32 GMT</pubDate>
      <guid>https://www.theguardian.com/food/2025/mar/01/nord-liverpool-its-very-much-a-win-restaurant-review</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/cf333e2b2f71df9fdbc0d8b4e08456adce83658c/0_537_8021_4813/master/8021.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=00f70f359152dba76754f3584ac7f95a">
        <media:credit scheme="urn:ebu">Photograph: Shaw and Shaw/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/cf333e2b2f71df9fdbc0d8b4e08456adce83658c/0_537_8021_4813/master/8021.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=afff87a379f35066e4581d8077a6d8b1">
        <media:credit scheme="urn:ebu">Photograph: Shaw and Shaw/The Observer</media:credit>
      </media:content>
      <dc:creator>Jay Rayner</dc:creator>
      <dc:date>2025-03-01T06:00:32Z</dc:date>
    </item>
    <item>
      <title>The truth about young men and sex: ‘We go along with things we’re uncomfortable with’</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/the-truth-about-young-men-and-sex</link>
      <description>&lt;p&gt;Do they really think about it every seven seconds? Are they only after one-night stands? We asked 30 men to bare all – their answers may surprise you&lt;/p&gt;&lt;p&gt;There’s a stereotype about young men and sex. I see it play out across social media, and over wine-fuelled chats with my friends: that men only want one thing, that they &lt;em&gt;all&lt;/em&gt; cheat and that, ultimately, they’re selfish in bed. It’s an idea that has been fuelled by the rise of toxic masculinity influencers such as &lt;a href="https://www.theguardian.com/news/andrew-tate"&gt;Andrew Tate&lt;/a&gt;, who discuss sex as something they are “owed” and encourage other men to think similarly.&lt;/p&gt;&lt;p&gt;For the past eight years, I’ve worked at Cosmopolitan magazine, speaking to millennial and gen-Z women about their love lives, and I can’t deny that there is &lt;em&gt;some&lt;/em&gt; truth to the stereotype. But when I&amp;nbsp;decided to have candid (and at times incredibly awkward) conversations with men in their 20s about&amp;nbsp;their sex lives, another story emerged: one of insecurity, hidden and misunderstood sexualities, and often a deeper need for connection.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/the-truth-about-young-men-and-sex"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/sex">Sex</category>
      <category domain="https://www.theguardian.com/lifeandstyle/men">Men</category>
      <category domain="https://www.theguardian.com/lifeandstyle/relationships">Relationships</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/world/lgbt-rights">LGBTQ+ rights</category>
      <category domain="https://www.theguardian.com/society/men">Men</category>
      <category domain="https://www.theguardian.com/society/sexuality">Sexuality</category>
      <category domain="https://www.theguardian.com/society/youngpeople">Young people</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Sat, 01 Mar 2025 09:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/the-truth-about-young-men-and-sex</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/83d91045827d80dece33ac1d9b489d7d1992c688/0_12_3307_1984/master/3307.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0d061ac035fe023584ec8d6657b21b8c">
        <media:credit scheme="urn:ebu">Illustration: Justin Metz/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/83d91045827d80dece33ac1d9b489d7d1992c688/0_12_3307_1984/master/3307.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=36ad352738870bfea2b19b2422511299">
        <media:credit scheme="urn:ebu">Illustration: Justin Metz/The Guardian</media:credit>
      </media:content>
      <dc:creator>Catriona Innes</dc:creator>
      <dc:date>2025-03-01T09:00:35Z</dc:date>
    </item>
    <item>
      <title>Parents in England: share your experiences of NHS dental services for your children</title>
      <link>https://www.theguardian.com/society/2025/feb/26/parents-in-england-share-your-experiences-of-nhs-dental-services-for-your-children</link>
      <description>&lt;p&gt;We would like to hear from parents about their children’s experiences of getting NHS dental treatment&lt;/p&gt;&lt;p&gt;According to a government report, nearly 50,000 tooth extractions took place last year in NHS hospitals in England for 0 to 19-year-olds, with 62% of those having a primary diagnosis of tooth decay.&lt;/p&gt;&lt;p&gt;We would like to hear from parents in England about their experiences of accessing NHS dental services for their children. Were you able to find somewhere locally or do you have to travel further afield? How easy have you found it to access care? We’re also interested in hearing from those whose children have had hospital tooth extractions recently.&lt;/p&gt; &lt;a href="https://www.theguardian.com/society/2025/feb/26/parents-in-england-share-your-experiences-of-nhs-dental-services-for-your-children"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/society/dentists">Dentists</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <pubDate>Wed, 26 Feb 2025 13:01:34 GMT</pubDate>
      <guid>https://www.theguardian.com/society/2025/feb/26/parents-in-england-share-your-experiences-of-nhs-dental-services-for-your-children</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5d7d3bbee7c205a69687710c1b0b9f52fd53057f/679_940_7273_4364/master/7273.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2bbbf77f38f879f8708de761972ee409">
        <media:credit scheme="urn:ebu">Photograph: Harbucks/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5d7d3bbee7c205a69687710c1b0b9f52fd53057f/679_940_7273_4364/master/7273.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2af06e9c85461f2da2496e79e826086a">
        <media:credit scheme="urn:ebu">Photograph: Harbucks/Getty Images</media:credit>
      </media:content>
      <dc:creator>Guardian community team</dc:creator>
      <dc:date>2025-02-26T13:01:34Z</dc:date>
    </item>
    <item>
      <title>Nature boys and girls – here’s your chance to get published in the Guardian</title>
      <link>https://www.theguardian.com/environment/2021/aug/27/nature-lovers-guardian-young-country-diary-writers</link>
      <description>&lt;p&gt;Our wildlife series Young Country Diary is looking for articles written by children, about their spring encounters with nature&lt;/p&gt;&lt;p&gt;Once again, the &lt;a href="https://www.theguardian.com/environment/series/young-country-diary"&gt;Young Country Diary series&lt;/a&gt; is open for submissions! Every three months, as the UK enters a new season, we ask you to send us an article written by a child aged 8-14.&lt;/p&gt;&lt;p&gt;The article needs to be about a &lt;strong&gt;recent encounter they’ve had with nature&lt;/strong&gt; – whether it’s a field of early spring flowers, a nest-building bird or a pond full of frogspawn.&lt;/p&gt; &lt;a href="https://www.theguardian.com/environment/2021/aug/27/nature-lovers-guardian-young-country-diary-writers"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/environment/wildlife">Wildlife</category>
      <category domain="https://www.theguardian.com/world/animals">Animals</category>
      <category domain="https://www.theguardian.com/environment/conservation">Conservation</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <pubDate>Fri, 28 Feb 2025 16:12:25 GMT</pubDate>
      <guid>https://www.theguardian.com/environment/2021/aug/27/nature-lovers-guardian-young-country-diary-writers</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/401acc09942d351f6de54c44f310b50c9113d231/108_35_1058_634/master/1058.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c86fde93a12f686cf147aa785acd482c">
        <media:credit scheme="urn:ebu">Photograph: Family handout</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/401acc09942d351f6de54c44f310b50c9113d231/108_35_1058_634/master/1058.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b51cc95af99407d36d1c876912f0f7a8">
        <media:credit scheme="urn:ebu">Photograph: Family handout</media:credit>
      </media:content>
      <dc:creator>Guardian community team</dc:creator>
      <dc:date>2025-02-28T16:12:25Z</dc:date>
    </item>
    <item>
      <title>Tell us: what’s the nicest thing a stranger has done for you?</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/feb/28/tell-us-what-is-the-nicest-thing-a-stranger-has-ever-done-for-you</link>
      <description>&lt;p&gt;We want to hear about chance encounters and acts of kindness that have restored your faith in community – or humanity&lt;/p&gt;&lt;p&gt;From wise words to good deeds, sometimes an interaction with a total stranger can be exactly what you need.&lt;/p&gt;&lt;p&gt;Guardian Australia is looking for readers willing to share their memorable moments with unfamiliar folk for our series &lt;a href="https://www.theguardian.com/lifeandstyle/series/kindness-of-strangers"&gt;Kindness of strangers&lt;/a&gt;.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/feb/28/tell-us-what-is-the-nicest-thing-a-stranger-has-ever-done-for-you"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/australian-lifestyle">Australian lifestyle</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Thu, 27 Feb 2025 22:30:38 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/feb/28/tell-us-what-is-the-nicest-thing-a-stranger-has-ever-done-for-you</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/76b6f7fc100b5bc1bfb601e5b2526bf06a4e4ec6/0_546_8192_4918/master/8192.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d64ab0211e8941e361e94fe497cc2ff5">
        <media:credit scheme="urn:ebu">Photograph: SolStock/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/76b6f7fc100b5bc1bfb601e5b2526bf06a4e4ec6/0_546_8192_4918/master/8192.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=eff4d03fe5cd607938eab618f588270c">
        <media:credit scheme="urn:ebu">Photograph: SolStock/Getty Images</media:credit>
      </media:content>
      <dc:creator>Guardian community team</dc:creator>
      <dc:date>2025-02-27T22:30:38Z</dc:date>
    </item>
    <item>
      <title>Tell us about the life-changing decisions you have made inspired by art</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/feb/27/tell-us-about-the-life-changing-decisions-you-have-made-inspired-by-art</link>
      <description>&lt;p&gt;We would like to hear from people who have uprooted their life for a piece of art&lt;/p&gt;&lt;p&gt;The Guardian’s Saturday magazine is looking for people who made a life-changing decision because they were inspired by some kind of art or culture.&lt;/p&gt;&lt;p&gt;Did you propose after listening to a particular song? Or &lt;a href="https://www.theguardian.com/film/2023/dec/02/i-left-the-cinema-walked-home-and-announced-i-was-moving-films-that-made-people-emigrate"&gt;move to New Zealand after seeing Lord of the Rings&lt;/a&gt;? Has a really great sex scene ever made you want to dump your boyfriend?&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/feb/27/tell-us-about-the-life-changing-decisions-you-have-made-inspired-by-art"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Thu, 27 Feb 2025 14:16:42 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/feb/27/tell-us-about-the-life-changing-decisions-you-have-made-inspired-by-art</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3a9ac73f078bebc0b59734921edddbe771b8c8cd/164_169_3281_1969/master/3281.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3854ad84f6365d145113360cfcceb9c1">
        <media:credit scheme="urn:ebu">Photograph: Praveen Menon/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3a9ac73f078bebc0b59734921edddbe771b8c8cd/164_169_3281_1969/master/3281.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=593f4cf0bea7f3974f9ba97c84e892ea">
        <media:credit scheme="urn:ebu">Photograph: Praveen Menon/Reuters</media:credit>
      </media:content>
      <dc:creator>Guardian community team</dc:creator>
      <dc:date>2025-02-27T14:16:42Z</dc:date>
    </item>
    <item>
      <title>‘There’s something wrong about it’: Santa Fe abuzz as residents wonder what caused Gene Hackman’s death</title>
      <link>https://www.theguardian.com/us-news/2025/mar/01/gene-hackman-death-santa-fe-new-mexico</link>
      <description>&lt;p&gt;New Mexico town shocked by deaths of actor, wife and dog – but answers to critical questions may take time to emerge&lt;/p&gt;&lt;p&gt;As New Mexico authorities investigate the deaths of Gene Hackman and his wife, Betsy Arakawa, their adopted home town of Santa Fe is grappling with the mystery of what happened to the couple.&lt;/p&gt;&lt;p&gt;Hackman, a Hollywood legend with two Academy Awards picked up over a 60-year career, and Arakawa, a classical pianist, had lived in the area for decades and had embraced the close-knit community that is New Mexico’s capital city.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/mar/01/gene-hackman-death-santa-fe-new-mexico"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/newmexico">New Mexico</category>
      <category domain="https://www.theguardian.com/film/gene-hackman">Gene Hackman</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/film/film">Film</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <pubDate>Sat, 01 Mar 2025 06:00:34 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/mar/01/gene-hackman-death-santa-fe-new-mexico</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/c7dd8ee7b45f7f43bf81619b4949b11155bae7d7/56_0_1667_1000/master/1667.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=981e06beeddf5abc20904e704feb9154">
        <media:credit scheme="urn:ebu">Photograph: Roberto E Rosales/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/c7dd8ee7b45f7f43bf81619b4949b11155bae7d7/56_0_1667_1000/master/1667.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=896d54a78155d7643d3f6e3d2ae3b373">
        <media:credit scheme="urn:ebu">Photograph: Roberto E Rosales/AP</media:credit>
      </media:content>
      <dc:creator>Dani Anguiano in Santa Fe</dc:creator>
      <dc:date>2025-03-01T06:00:34Z</dc:date>
    </item>
    <item>
      <title>‘Trump is abandoning Ukraine and wants a weaker EU’: Dominique de Villepin on Europe’s moment of truth</title>
      <link>https://www.theguardian.com/commentisfree/2025/mar/01/trump-is-abandoning-ukraine-and-wants-a-weaker-eu-dominique-de-villepin-on-europes-moment-of-truth</link>
      <description>&lt;p&gt;The former French PM says the US is no longer an ally of Europe – but has joined Russia and China as an ‘illiberal superpower’&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Live reaction to Zelenskyy’s clash with Trump and Vance&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Dominique de Villepin made his name with a&lt;a href="https://en.wikisource.org/wiki/French_address_on_Iraq_at_the_UN_Security_Council"&gt; memorable speech&lt;/a&gt; to the UN security council in February 2003, just before the US-led invasion of Iraq. De Villepin, the then French foreign minister, in effect signalled France’s intention to veto a&lt;strong&gt; &lt;/strong&gt;UN resolution &lt;a href="https://en.wikipedia.org/wiki/United_Nations_Security_Council_and_the_Iraq_War"&gt;authorising the war&lt;/a&gt;, forcing the US and UK to act unilaterally. He warned that Washington’s strategy would lead to chaos in the Middle East and undermine international institutions. The prophetic plea was met with applause, a rare event in the security council chamber. It led to the career diplomat’s inclusion as a character in David Hare’s 2004 anti-war play, &lt;a href="https://www.theguardian.com/stage/2004/sep/11/theatre.politicaltheatre"&gt;Stuff Happens&lt;/a&gt;.&lt;/p&gt;&lt;p&gt;Now the veteran statesman, who warned about the risks of Europe’s over-reliance on the US many years before it became a mainstream opinion in Paris or Berlin, is back with advice on how to respond to the most serious breakdown in Europe’s relationship with the US in 80 years.&lt;/p&gt; &lt;a href="https://www.theguardian.com/commentisfree/2025/mar/01/trump-is-abandoning-ukraine-and-wants-a-weaker-eu-dominique-de-villepin-on-europes-moment-of-truth"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/france">France</category>
      <category domain="https://www.theguardian.com/world/dominique-de-villepin">Dominique de Villepin</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/eu">European Union</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <pubDate>Sat, 01 Mar 2025 06:00:34 GMT</pubDate>
      <guid>https://www.theguardian.com/commentisfree/2025/mar/01/trump-is-abandoning-ukraine-and-wants-a-weaker-eu-dominique-de-villepin-on-europes-moment-of-truth</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b129a39f8ab7207041ba93346bd1f842d319c66f/558_119_3610_2166/master/3610.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=1f529d35b1d3d9fca210050c7b6f7943">
        <media:credit scheme="urn:ebu">Photograph: Ludovic Marin/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b129a39f8ab7207041ba93346bd1f842d319c66f/558_119_3610_2166/master/3610.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2daf815c601c2531c2864a4ff18b1f54">
        <media:credit scheme="urn:ebu">Photograph: Ludovic Marin/AP</media:credit>
      </media:content>
      <dc:creator>Martin Gelin</dc:creator>
      <dc:date>2025-03-01T06:00:34Z</dc:date>
    </item>
    <item>
      <title>‘They’ve lost my trust’: consumers shun companies as bosses kowtow to Trump</title>
      <link>https://www.theguardian.com/money/2025/feb/28/trump-consumer-protests-companies</link>
      <description>&lt;p&gt;Americans are using their wallet to hurt where it matters – including during Friday’s planned ‘economic blackout’&lt;/p&gt;&lt;ul&gt;&lt;li&gt;Don’t let a billionaire’s algorithm control what you read. &lt;a href="https://app.adjust.com/1ja835wd"&gt;Download our free app to get trusted reporting&lt;/a&gt;.&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;In late January, Lauren Bedson did what many would likely find unthinkable: she cancelled her &lt;a href="https://www.theguardian.com/technology/amazon"&gt;Amazon&lt;/a&gt; Prime membership. The catalyst was &lt;a href="https://www.theguardian.com/us-news/donaldtrump"&gt;Donald Trump’s&lt;/a&gt; inauguration. Many more Americans are planning to make similar decisions &lt;a href="https://www.npr.org/2025/02/27/nx-s1-5311972/economic-blackout-february-28-explainer"&gt;this Friday&lt;/a&gt;.&lt;/p&gt;&lt;p&gt;Bedson made her move after seeing photos of Jeff Bezos, the Amazon founder, sitting with other tech moguls and billionaires, including Elon Musk, Mark Zuckerberg and Google’s Sundar Pichai, just rows behind Trump at &lt;a href="https://www.theguardian.com/us-news/2025/jan/20/trump-inauguration-tech-executives"&gt;his inauguration.&lt;/a&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/money/2025/feb/28/trump-consumer-protests-companies"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/money/consumer-affairs">Consumer affairs</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <category domain="https://www.theguardian.com/technology/amazon">Amazon</category>
      <category domain="https://www.theguardian.com/technology/technology">Technology</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/business/wal-mart">Walmart</category>
      <category domain="https://www.theguardian.com/business/retail">Retail industry</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/technology/meta">Meta</category>
      <category domain="https://www.theguardian.com/technology/facebook">Facebook</category>
      <category domain="https://www.theguardian.com/technology/google">Google</category>
      <category domain="https://www.theguardian.com/technology/jeff-bezos">Jeff Bezos</category>
      <category domain="https://www.theguardian.com/technology/mark-zuckerberg">Mark Zuckerberg</category>
      <category domain="https://www.theguardian.com/money/money">Money</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <pubDate>Fri, 28 Feb 2025 11:00:08 GMT</pubDate>
      <guid>https://www.theguardian.com/money/2025/feb/28/trump-consumer-protests-companies</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3dd942d0c6435de5ae44e200e69ea5ea55c46a9e/0_378_6000_3599/master/6000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=bd1cdb40112fc632b3da6ef49f1f1414">
        <media:credit scheme="urn:ebu">Photograph: Andrew Harnik/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3dd942d0c6435de5ae44e200e69ea5ea55c46a9e/0_378_6000_3599/master/6000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=5a8a6dc45ad71bfaef2c3acf2f7f3b4b">
        <media:credit scheme="urn:ebu">Photograph: Andrew Harnik/Getty Images</media:credit>
      </media:content>
      <dc:creator>Lauren Aratani in New York and Guardian community team</dc:creator>
      <dc:date>2025-02-28T11:00:08Z</dc:date>
    </item>
    <item>
      <title>‘I am willing to die’: hunger-striking mother of writer jailed in Egypt fights on in London hospital bed</title>
      <link>https://www.theguardian.com/politics/2025/feb/28/mother-of-egypt-detainee-agrees-to-glucose-drip-after-150-day-hunger-strike</link>
      <description>&lt;p&gt;After more than 150 days without food, Laila Soueif says she will continue until there is some positive news from Cairo &lt;/p&gt;&lt;p&gt;Laila Soueif, lying in a hospital bed after &lt;a href="https://www.theguardian.com/politics/2025/feb/26/mother-of-egypt-detainee-alaa-abd-el-fattah-hunger-strike-uk"&gt;refusing all food for 152 days in a bid to free her jailed son&lt;/a&gt;, agreed on Wednesday night to be put on a glucose drip, although it is only likely to delay her full collapse by days.&lt;/p&gt;&lt;p&gt;She said she had taken the step as part of a deal she had reached with her children that they would be allowed one chance to intervene before she collapses.&lt;/p&gt; &lt;a href="https://www.theguardian.com/politics/2025/feb/28/mother-of-egypt-detainee-agrees-to-glucose-drip-after-150-day-hunger-strike"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/politics/foreignpolicy">Foreign policy</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <category domain="https://www.theguardian.com/world/egypt">Egypt</category>
      <category domain="https://www.theguardian.com/uk/london">London</category>
      <category domain="https://www.theguardian.com/uk-news/england">England</category>
      <pubDate>Fri, 28 Feb 2025 17:48:02 GMT</pubDate>
      <guid>https://www.theguardian.com/politics/2025/feb/28/mother-of-egypt-detainee-agrees-to-glucose-drip-after-150-day-hunger-strike</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/a8b92bcb5ce008c991e1a297c54792898d6dcf7e/0_443_1007_604/master/1007.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=17e12cf821d755ff1496aafc7344537c">
        <media:credit scheme="urn:ebu">Photograph: supplied</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/a8b92bcb5ce008c991e1a297c54792898d6dcf7e/0_443_1007_604/master/1007.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9269ca4480ecdd571c126c04010fc991">
        <media:credit scheme="urn:ebu">Photograph: supplied</media:credit>
      </media:content>
      <dc:creator>Patrick Wintour</dc:creator>
      <dc:date>2025-02-28T17:48:02Z</dc:date>
    </item>
    <item>
      <title>‘He looks half the man he was’: brother of Israeli hostage on seeing him pleading for release</title>
      <link>https://www.theguardian.com/world/2025/feb/28/brother-of-evyatar-david-israeli-hostage-hamas-video</link>
      <description>&lt;p&gt;Sighting of Evyatar David in Hamas video brought joy to his brother Ilay but his appearance indicated his suffering&lt;/p&gt;&lt;p&gt;When Hamas brought two captive Israelis to watch Saturday’s release of six of their fellow hostages and then beg for their own liberation, it was purposely jabbing at a deeply painful divide in Israeli society.&lt;/p&gt;&lt;p&gt;Evyatar David and Guy Gilboa-Dalal were 22-year-old best friends when they were kidnapped at the Nova music festival on 7 October 2023. The Hamas video showed them sitting in a minivan watching the &lt;a href="https://www.theguardian.com/world/video/2025/feb/22/israeli-hostages-released-by-hamas-in-rafah-ceasefire-deal-video"&gt;propaganda-laden handover ceremony&lt;/a&gt;, and then turning to the cameras to plead with Benjamin Netanyahu to agree a &lt;a href="https://www.theguardian.com/world/2025/feb/26/hamas-gives-up-bodies-of-four-hostages-as-ceasefire-appears-to-get-back-on-track"&gt;second phase in the current ceasefire&lt;/a&gt;, which would allow the release of all the remaining 59 hostages (only a minority of whom are thought to be still alive).&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/feb/28/brother-of-evyatar-david-israeli-hostage-hamas-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/israel-hamas-war">Israel-Gaza war</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/world/hamas">Hamas</category>
      <category domain="https://www.theguardian.com/world/gaza">Gaza</category>
      <category domain="https://www.theguardian.com/world/benjamin-netanyahu">Benjamin Netanyahu</category>
      <category domain="https://www.theguardian.com/world/palestinian-territories">Palestinian territories</category>
      <category domain="https://www.theguardian.com/world/middleeast">Middle East and north Africa</category>
      <pubDate>Fri, 28 Feb 2025 14:35:22 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/feb/28/brother-of-evyatar-david-israeli-hostage-hamas-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/dfc84d2d7289878a840e80405f2d08d8b7d8ce60/0_118_6240_3744/master/6240.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=86d4ea4657b55950375e3ee4d5183b1e">
        <media:credit scheme="urn:ebu">Photograph: Quique Kierszenbaum/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/dfc84d2d7289878a840e80405f2d08d8b7d8ce60/0_118_6240_3744/master/6240.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=6873e9b9facd0ecb4267bdaa07bb651b">
        <media:credit scheme="urn:ebu">Photograph: Quique Kierszenbaum/The Guardian</media:credit>
      </media:content>
      <dc:creator>Julian Borger and Quique Kierszenbaum in Jerusalem</dc:creator>
      <dc:date>2025-02-28T14:35:22Z</dc:date>
    </item>
    <item>
      <title>‘We brought him 4kg of ice-cream’: Pope Francis’s parlour shares papal favourites</title>
      <link>https://www.theguardian.com/world/2025/feb/28/we-brought-him-4kg-of-ice-cream-pope-franciss-parlour-shares-papal-favourites</link>
      <description>&lt;p&gt;Sebastian Padrón explains how ice-cream brought his family closer with fellow Argentinian and neighbour&lt;/p&gt;&lt;p&gt;When Sebastian Padrón opened his ice-cream parlour around the corner from Pope Francis’s home in Casa Santa Marta in Vatican City, his wife, Silvia, came up with a clever way of ingratiating her husband with his fellow Argentinian.&lt;/p&gt;&lt;p&gt;“She told me: ‘Go and bring an ice-cream to Pope Francis,’” said Padrón. “I said: ‘Impossible.’”&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/2025/feb/28/we-brought-him-4kg-of-ice-cream-pope-franciss-parlour-shares-papal-favourites"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/pope-francis">Pope Francis</category>
      <category domain="https://www.theguardian.com/world/italy">Italy</category>
      <category domain="https://www.theguardian.com/world/catholicism">Catholicism</category>
      <category domain="https://www.theguardian.com/world/christianity">Christianity</category>
      <category domain="https://www.theguardian.com/world/religion">Religion</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/the-papacy">The papacy</category>
      <category domain="https://www.theguardian.com/world/vatican">Vatican</category>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/food/ice-cream">Ice-cream and sorbet</category>
      <pubDate>Fri, 28 Feb 2025 13:24:11 GMT</pubDate>
      <guid>https://www.theguardian.com/world/2025/feb/28/we-brought-him-4kg-of-ice-cream-pope-franciss-parlour-shares-papal-favourites</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/810c62ed05d156da530f16a31cf4899b7b2a1938/0_300_5918_3551/master/5918.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0b317957b04393f4dac57104bfcc9a1e">
        <media:credit scheme="urn:ebu">Photograph: Victor Sokolowicz/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/810c62ed05d156da530f16a31cf4899b7b2a1938/0_300_5918_3551/master/5918.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=891ed530980ab6889fc0dc2dfc39a73b">
        <media:credit scheme="urn:ebu">Photograph: Victor Sokolowicz/The Guardian</media:credit>
      </media:content>
      <dc:creator>Angela Giuffrida in Rome</dc:creator>
      <dc:date>2025-02-28T13:24:11Z</dc:date>
    </item>
    <item>
      <title>‘Horrendous’: the ‘ridiculously common’ lies people tell on CVs, and what happens when they are discovered</title>
      <link>https://www.theguardian.com/law/2025/mar/01/uk-recruiters-job-applications-cv-falsification-lies</link>
      <description>&lt;p&gt;Fake reasons for leaving jobs, manipulated dates and inflated titles among most frequent falsehoods&lt;/p&gt;&lt;p&gt;In 28 years of recruitment, Matt Collingwood has witnessed some “very awkward” job interviews. Like the candidate whose CV falsely boasted of a second-dan black belt in taekwondo, only to discover his interviewer was an aficionado of the sport. “An interview that should have been an hour lasted 15 minutes,” said Collingwood, the managing director of the IT recruitment agency Viqu.&lt;/p&gt;&lt;p&gt;Or the candidate who claimed he had attended a certain private school, which his interviewer had also attended and would have been in the year above. But when asked for teachers’ names, the school motto, even where the sports field was, “he was clueless. Didn’t get the job.”&lt;/p&gt; &lt;a href="https://www.theguardian.com/law/2025/mar/01/uk-recruiters-job-applications-cv-falsification-lies"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/law/employment-law">Employment law</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/education/careerseducation">Careers</category>
      <category domain="https://www.theguardian.com/technology/artificialintelligenceai">Artificial intelligence (AI)</category>
      <pubDate>Sat, 01 Mar 2025 08:01:53 GMT</pubDate>
      <guid>https://www.theguardian.com/law/2025/mar/01/uk-recruiters-job-applications-cv-falsification-lies</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/6c1f0badb6fdd90c861b88f7f8335fd5bc43da52/0_0_5456_3273/master/5456.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=db26729662922979d39352a498e8f4bd">
        <media:credit scheme="urn:ebu">Photograph: Tero Vesalainen/Alamy</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/6c1f0badb6fdd90c861b88f7f8335fd5bc43da52/0_0_5456_3273/master/5456.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=eec0b1c1f75d14bec51ef82bc0b8e5f8">
        <media:credit scheme="urn:ebu">Photograph: Tero Vesalainen/Alamy</media:credit>
      </media:content>
      <dc:creator>Caroline Davies</dc:creator>
      <dc:date>2025-03-01T08:01:53Z</dc:date>
    </item>
    <item>
      <title>Former Barclays boss gambles on courtroom battle over ties to Epstein</title>
      <link>https://www.theguardian.com/business/2025/mar/01/former-barclays-boss-gambles-on-courtroom-battle-over-ties-to-epstein</link>
      <description>&lt;p&gt;Next week Jes Staley will challenge his ban from the City in a case that will reveal his conversations with the disgraced financier&lt;/p&gt;&lt;p&gt;Former Barclays chief executive Jes Staley is about to take a major risk.&lt;/p&gt;&lt;p&gt;Nearly two and a half years after he was banned from the City for allegedly lying about the extent of his relationship with Jeffrey Epstein, the former banking boss is hoping to convince judges to overturn the reputation-shattering ruling.&lt;/p&gt; &lt;a href="https://www.theguardian.com/business/2025/mar/01/former-barclays-boss-gambles-on-courtroom-battle-over-ties-to-epstein"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/business/banking">Banking</category>
      <category domain="https://www.theguardian.com/business/business">Business</category>
      <category domain="https://www.theguardian.com/business/barclay">Barclays</category>
      <category domain="https://www.theguardian.com/us-news/jeffrey-epstein">Jeffrey Epstein</category>
      <category domain="https://www.theguardian.com/business/financial-conduct-authority">Financial Conduct Authority</category>
      <category domain="https://www.theguardian.com/business/regulators">Regulators</category>
      <pubDate>Sat, 01 Mar 2025 16:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/business/2025/mar/01/former-barclays-boss-gambles-on-courtroom-battle-over-ties-to-epstein</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d7838392d63a26436eca27946fb65cac4f4a2d05/0_24_2072_1243/master/2072.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=aa4677c69cbda4ed781eea8054b0adb6">
        <media:credit scheme="urn:ebu">Photograph: Peter Nicholls/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d7838392d63a26436eca27946fb65cac4f4a2d05/0_24_2072_1243/master/2072.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e9d889a536500735db8de86d34ca125b">
        <media:credit scheme="urn:ebu">Photograph: Peter Nicholls/Reuters</media:credit>
      </media:content>
      <dc:creator>Kalyeena Makortoff Banking correspondent</dc:creator>
      <dc:date>2025-03-01T16:00:43Z</dc:date>
    </item>
    <item>
      <title>Diplomacy dies on live TV as Trump and Vance gang up to bully Ukraine leader</title>
      <link>https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-shouting-match-oval-office</link>
      <description>&lt;p&gt;US president said his horrific blow-up would make ‘great television’ – the White House has never seen anything like it&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/world/live/2025/mar/01/live-european-leaders-rally-behind-zelenskyy-after-trump-vance-clash-updates"&gt;Live reaction to Zelenskyy’s clash with Trump and Vance&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;“This is going to be great television,” &lt;a href="https://www.theguardian.com/us-news/donaldtrump"&gt;Donald Trump&lt;/a&gt; remarked at the end. Sure. And as they slipped into the icy depths, the captain of the Titanic probably assured his passengers that this would make a great movie some day.&lt;/p&gt;&lt;p&gt;Trump on Friday presided over one of the greatest diplomatic disasters in modern history. Tempers flared, voices were raised and protocol was shredded in the once-hallowed Oval Office. &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-meeting-ukraine-aid-war"&gt;As Trump got into a shouting match with Ukraine’s Volodymyr Zelenskyy,&lt;/a&gt; a horrified Europe watched the post-second world war order crumble before its eyes.&lt;/p&gt; &lt;a href="https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-shouting-match-oval-office"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/us-news/jd-vance">JD Vance</category>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/vladimir-putin">Vladimir Putin</category>
      <pubDate>Fri, 28 Feb 2025 20:52:36 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/2025/feb/28/trump-zelenskyy-shouting-match-oval-office</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/51a0f3de50f78494bb70bea90dffacd180ac0652/0_49_4741_2846/master/4741.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c03d33c64883bc4cbb07b13f3963d6d3">
        <media:credit scheme="urn:ebu">Photograph: Brian Snyder/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/51a0f3de50f78494bb70bea90dffacd180ac0652/0_49_4741_2846/master/4741.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c2f17791cfc80ee8c304feb489ee32fb">
        <media:credit scheme="urn:ebu">Photograph: Brian Snyder/Reuters</media:credit>
      </media:content>
      <dc:creator>David Smith in Washington</dc:creator>
      <dc:date>2025-02-28T20:52:36Z</dc:date>
    </item>
    <item>
      <title>‘I felt like I was his carer’: why straight women in relationships lose interest in sex</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/i-felt-like-i-was-his-carer-why-straight-women-in-relationships-lose-interest-in-sex</link>
      <description>&lt;p&gt;In unequal households – the majority of heterosexual homes – domestic and emotional pressures on women can have a direct effect on libido&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.theguardian.com/newsletters/2019/oct/18/saved-for-later-sign-up-for-guardian-australias-culture-and-lifestyle-email?CMP=cvau_sfl"&gt;Get our weekend culture and lifestyle email&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Zoe and her husband, Charles, can’t keep their hands off each other. They were like this in the early stages of their relationship, too – “there was something wrong with us” – Zoe jokes about their prolific lovemaking. But this new, “giddy” phase is different.&lt;/p&gt;&lt;p&gt;“It feels like we’ve just started again. But with all this history, and this amazing child, and all this other stuff that binds us together,” she says.&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/i-felt-like-i-was-his-carer-why-straight-women-in-relationships-lose-interest-in-sex"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/sex">Sex</category>
      <category domain="https://www.theguardian.com/society/women">Women</category>
      <category domain="https://www.theguardian.com/lifeandstyle/women">Women</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <pubDate>Fri, 28 Feb 2025 14:00:15 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/i-felt-like-i-was-his-carer-why-straight-women-in-relationships-lose-interest-in-sex</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/0f081a0b432bf88894bbfc254ca78152b5bd5663/0_315_7360_4415/master/7360.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e7f4660c93f36696e75397616c9e06ad">
        <media:credit scheme="urn:ebu">Photograph: Francesco Carta fotografo/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/0f081a0b432bf88894bbfc254ca78152b5bd5663/0_315_7360_4415/master/7360.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3ef9a25e04a7136c8e99dc5b43a00c1c">
        <media:credit scheme="urn:ebu">Photograph: Francesco Carta fotografo/Getty Images</media:credit>
      </media:content>
      <dc:creator>Alyx Gorman</dc:creator>
      <dc:date>2025-02-28T14:00:15Z</dc:date>
    </item>
    <item>
      <title>The White Lotus actor Aimee Lou Wood, Marina Hyde on Daddy Musk, and are we over-diagnosing illness? – podcast</title>
      <link>https://www.theguardian.com/lifeandstyle/audio/2025/mar/01/the-white-lotus-aimee-lou-wood-marina-hyde-on-daddy-musk-are-we-over-diagnosing-illness-podcast</link>
      <description>&lt;p&gt;With the mothers of Elon’s kids begging for his attention on social media, he makes much of ‘pronatalism’ – but is that just a fancy word for bad parenting? ‘I don’t know whether I’d describe it as fun,’ says Aimee Lou Wood on the intensity of making The White Lotus. And are ordinary life experiences, bodily imperfections and normal differences being unnecessarily pathologised? Neurologist and author Suzanne O’Sullivan argues just that&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/audio/2025/mar/01/the-white-lotus-aimee-lou-wood-marina-hyde-on-daddy-musk-are-we-over-diagnosing-illness-podcast"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/culture/aimee-lou-wood">Aimee Lou Wood</category>
      <category domain="https://www.theguardian.com/tv-and-radio/sex-education">Sex Education</category>
      <category domain="https://www.theguardian.com/tv-and-radio/the-white-lotus">The White Lotus</category>
      <category domain="https://www.theguardian.com/technology/elon-musk">Elon Musk</category>
      <category domain="https://www.theguardian.com/lifeandstyle/parents-and-parenting">Parents and parenting</category>
      <category domain="https://www.theguardian.com/society/attention-deficit-hyperactivity-disorder">Attention deficit hyperactivity disorder</category>
      <category domain="https://www.theguardian.com/society/neurodiversity">Neurodiversity</category>
      <category domain="https://www.theguardian.com/society/autism">Autism</category>
      <category domain="https://www.theguardian.com/society/long-covid">Long Covid</category>
      <pubDate>Sat, 01 Mar 2025 05:00:29 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/audio/2025/mar/01/the-white-lotus-aimee-lou-wood-marina-hyde-on-daddy-musk-are-we-over-diagnosing-illness-podcast</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d74ea0dea3607075789fbe55633aeef686b79eea/0_1758_6192_3715/master/6192.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=0aa32e9ae7700a97343062a6b927b736">
        <media:credit scheme="urn:ebu">Photograph: Hollie Fernando/The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d74ea0dea3607075789fbe55633aeef686b79eea/0_1758_6192_3715/master/6192.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3f1fc3e1b9a6620e2ffed762b3b3b9ec">
        <media:credit scheme="urn:ebu">Photograph: Hollie Fernando/The Guardian</media:credit>
      </media:content>
      <dc:creator>Hosted by Savannah Ayoade-Greaves; written by Marina Hyde, Zoe Williams, and Suzanne O'Sullivan. Narrated by Evelyn Miller and Ciara Baxendale. Produced by Rachel Porter; the executive producer was Ellie Bury.</dc:creator>
      <dc:date>2025-03-01T05:00:29Z</dc:date>
    </item>
    <item>
      <title>Special episode: Inside the room when Starmer met Trump – Politics Weekly Westminster</title>
      <link>https://www.theguardian.com/politics/audio/2025/feb/28/special-episode-inside-the-room-when-starmer-met-trump-politics-weekly-westminster</link>
      <description>&lt;p&gt;Pippa Crerar and Kiran Stacey have a behind-the-scenes look at Keir Starmer’s trip to meet Donald Trump at the White House, after Pippa travelled with the prime minister to Washington DC. So, how was Starmer’s charm offensive received by the president? And has the trip moved the dial on Ukraine and tariffs?&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;strong&gt;Send your questions and feedback to &lt;a href="mailto:podcasts@theguardian.com"&gt;politicsweeklyuk@theguardian.com&lt;/a&gt;&lt;/strong&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/politics/audio/2025/feb/28/special-episode-inside-the-room-when-starmer-met-trump-politics-weekly-westminster"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/politics/politics">Politics</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Fri, 28 Feb 2025 16:00:13 GMT</pubDate>
      <guid>https://www.theguardian.com/politics/audio/2025/feb/28/special-episode-inside-the-room-when-starmer-met-trump-politics-weekly-westminster</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/f579746b80da807814f5e1cbab6c609171b8ee87/0_0_1600_960/master/1600.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a03053030063989320a1590e583d4626">
        <media:credit scheme="urn:ebu">Photograph: Simon Dawson/No10 Downing Street</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/f579746b80da807814f5e1cbab6c609171b8ee87/0_0_1600_960/master/1600.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=219d64d841a0c164b9e152873fb71260">
        <media:credit scheme="urn:ebu">Photograph: Simon Dawson/No10 Downing Street</media:credit>
      </media:content>
      <dc:creator>Presented by Pippa Crerar and Kiran Stacey, produced by Frankie Tobi, music by Axel Kacoutié; the executive producer is Zoe Hitch</dc:creator>
      <dc:date>2025-02-28T16:00:13Z</dc:date>
    </item>
    <item>
      <title>Israel and the delusions of Germany’s ‘memory culture’ – podcast</title>
      <link>https://www.theguardian.com/news/audio/2025/feb/28/israel-and-the-delusions-of-germanys-memory-culture-podcast</link>
      <description>&lt;p&gt;Germany embraced Israel to atone for its wartime guilt. But was this in part a way to avoid truly confronting its past? By Pankaj Mishra. Read by Mikhail Sen&lt;/p&gt; &lt;a href="https://www.theguardian.com/news/audio/2025/feb/28/israel-and-the-delusions-of-germanys-memory-culture-podcast"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/germany">Germany</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/world/judaism">Judaism</category>
      <category domain="https://www.theguardian.com/news/antisemitism">Antisemitism</category>
      <category domain="https://www.theguardian.com/world/israel-hamas-war">Israel-Gaza war</category>
      <category domain="https://www.theguardian.com/world/nazism">Nazism</category>
      <pubDate>Fri, 28 Feb 2025 05:00:49 GMT</pubDate>
      <guid>https://www.theguardian.com/news/audio/2025/feb/28/israel-and-the-delusions-of-germanys-memory-culture-podcast</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/5f5b04ccd96f28edb59ce23bb8c8ce86f2ae698a/0_410_7744_4646/master/7744.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2c2e81cf717a7bbd9bce269b3a9638f6">
        <media:credit scheme="urn:ebu">Photograph: Anadolu/Anadolu Agency/Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/5f5b04ccd96f28edb59ce23bb8c8ce86f2ae698a/0_410_7744_4646/master/7744.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=e220408ceecfa17e026caa321619c4fa">
        <media:credit scheme="urn:ebu">Photograph: Anadolu/Anadolu Agency/Getty Images</media:credit>
      </media:content>
      <dc:creator>Written by Pankaj Mishra and read by Mikhail Sen. Produced by Nicola Alexandrou. The executive producer was Phil Maynard</dc:creator>
      <dc:date>2025-02-28T05:00:49Z</dc:date>
    </item>
    <item>
      <title>Joy, hope and murder in free Syria – podcast</title>
      <link>https://www.theguardian.com/news/audio/2025/feb/28/joy-hope-and-murder-in-free-syria-podcast</link>
      <description>&lt;p&gt;Syria has a new leader, and for thousands it is a time of celebration and optimism. But old enmities and fears about what comes next haunt the country. &lt;strong&gt;Michael Safi&lt;/strong&gt; reports&lt;/p&gt;&lt;p&gt;After more than a decade of war, and half a century of repressive rule under Bashar al-Assad and his father, Syrians have a new ruler and a new future. &lt;strong&gt;Michael Safi&lt;/strong&gt; spent a week travelling around the country, speaking to people about their surging hopes and joy – but also their fears of how fragile this peace could prove to be.&lt;/p&gt;&lt;p&gt;Driving from Lebanon to Damascus with a family, he heard about the painful toll the years of war and repression had taken on them: a father killed, a brother disappeared, a sister jailed. But they also told him how optimistic they still were for this moment of history.&lt;/p&gt; &lt;a href="https://www.theguardian.com/news/audio/2025/feb/28/joy-hope-and-murder-in-free-syria-podcast"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/syria">Syria</category>
      <category domain="https://www.theguardian.com/world/bashar-al-assad">Bashar al-Assad</category>
      <pubDate>Fri, 28 Feb 2025 03:00:44 GMT</pubDate>
      <guid>https://www.theguardian.com/news/audio/2025/feb/28/joy-hope-and-murder-in-free-syria-podcast</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/8aa7300740e42667dfab1e02f40126bcfe78edbf/0_0_5500_3300/master/5500.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=683edd5d70c5b9b5c6e0d2e2d27887ac">
        <media:credit scheme="urn:ebu">Photograph: Ammar Awad/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/8aa7300740e42667dfab1e02f40126bcfe78edbf/0_0_5500_3300/master/5500.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b915eb4e7e70343600f46b80a3f58489">
        <media:credit scheme="urn:ebu">Photograph: Ammar Awad/Reuters</media:credit>
      </media:content>
      <dc:creator>Presented by Michael Safi; produced by Alex Atackand Tom Glasser; executive producer Homa Khaleeli</dc:creator>
      <dc:date>2025-02-28T03:00:44Z</dc:date>
    </item>
    <item>
      <title>Is the tide starting to turn against Trump? – podcast</title>
      <link>https://www.theguardian.com/politics/audio/2025/feb/27/is-the-tide-starting-to-turn-against-trump-podcast</link>
      <description>&lt;p&gt;This week, Donald Trump continued to dominate the world stage, welcoming a procession of global leaders to Washington, including Keir Starmer. But while the ‘special relationship’ is front and centre in the UK, attention in the US is very much elsewhere. As the president goes full steam ahead with his domestic agenda, there are warning signs for Trump in the polls. So, could he be in trouble at home? And how could the Democrats take advantage?&lt;/p&gt;&lt;p&gt;Jonathan Freedland speaks to Stanley Greenberg, the bestselling author, Democratic pollster and political strategist who played a crucial role in the elections of Bill Clinton and Tony Blair&lt;/p&gt; &lt;a href="https://www.theguardian.com/politics/audio/2025/feb/27/is-the-tide-starting-to-turn-against-trump-podcast"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/us-politics">US politics</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Thu, 27 Feb 2025 23:21:06 GMT</pubDate>
      <guid>https://www.theguardian.com/politics/audio/2025/feb/27/is-the-tide-starting-to-turn-against-trump-podcast</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/bc951d5382b4cdeb7d6e560bec3eb56206eb0477/0_0_4144_2487/master/4144.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=bb1ba32384941892b19af1b4487776b7">
        <media:credit scheme="urn:ebu">Photograph: Ben Curtis/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/bc951d5382b4cdeb7d6e560bec3eb56206eb0477/0_0_4144_2487/master/4144.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=1b5a8e3d35803e34952e7cb8e7b828c4">
        <media:credit scheme="urn:ebu">Photograph: Ben Curtis/AP</media:credit>
      </media:content>
      <dc:creator>Presented by Jonathan Freedland with Stan Greenberg. Produced by Hattie Moir; the executive producer is Zoe Hitch</dc:creator>
      <dc:date>2025-02-27T23:21:06Z</dc:date>
    </item>
    <item>
      <title>How China uses ‘salami-slicing’ tactics to exert pressure on Taiwan – video</title>
      <link>https://www.theguardian.com/world/video/2025/feb/28/how-china-uses-salami-slicing-tactics-to-exert-pressure-on-taiwan-video</link>
      <description>&lt;p&gt;China has dramatically increased military activities around Taiwan, with more than 3,000 incursions into Taiwan's airspace in 2024 alone. Amy Hawkins examines how Beijing is deploying 'salami-slicing' tactics, a strategy of gradual pressure that stays below the threshold of war while steadily wearing down Taiwan's defences. From daily air incursions to strategic military exercises, we explore the four phases of China's approach and what it means for Taiwan's future&lt;/p&gt;&lt;p&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/world/video/2025/feb/28/how-china-uses-salami-slicing-tactics-to-exert-pressure-on-taiwan-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/taiwan">Taiwan</category>
      <category domain="https://www.theguardian.com/world/china">China</category>
      <category domain="https://www.theguardian.com/world/asia-pacific">Asia Pacific</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Fri, 28 Feb 2025 09:14:05 GMT</pubDate>
      <guid>https://www.theguardian.com/world/video/2025/feb/28/how-china-uses-salami-slicing-tactics-to-exert-pressure-on-taiwan-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/711df1267553fec00085fb0f5fab8b9711265da1/60_0_1800_1080/master/1800.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9bc63d3db4c4fdadcf21e1561be71486">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/711df1267553fec00085fb0f5fab8b9711265da1/60_0_1800_1080/master/1800.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=806995a17b2689df4d75b385331d4b59">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <dc:creator>Amy Hawkins Jem Talbot Elena Morresi Ryan Baxter Ali Assaf</dc:creator>
      <dc:date>2025-02-28T09:14:05Z</dc:date>
    </item>
    <item>
      <title>'Did I say that?': Donald Trump denies calling Zelenskyy a dictator even though he did – video</title>
      <link>https://www.theguardian.com/us-news/video/2025/feb/27/did-i-say-that-donald-trump-denies-calling-zelenskyy-a-dictator-video</link>
      <description>&lt;p&gt;The US president, Donald Trump, denied calling the Ukrainian president, Volodymyr Zelenskyy, a dictator, despite calling him one on his social media platform, Truth Social. Trump was asked by a reporter if he still held that view in a press conference alongside the British prime minister, Keir Starmer, and he replied: 'Did I say that? I can't believe I said that'&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/us-news/2025/feb/27/trump-starmer-meeting"&gt;Donald Trump’s meeting with Keir Starmer: key takeaways&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/us-news/2025/feb/27/king-charles-invites-donald-trump-for-unprecedented-second-state-visit-to-uk"&gt;King Charles invites Donald Trump for unprecedented second state visit to UK&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/politics/live/2025/feb/27/keir-starmer-donald-trump-white-house-ukraine-uk-politics-live-news"&gt;Trump praises Starmer’s ‘hard’ lobbying as he again suggests UK will be exempt from US tariffs – live&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/us-news/video/2025/feb/27/did-i-say-that-donald-trump-denies-calling-zelenskyy-a-dictator-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/politics/keir-starmer">Keir Starmer</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Thu, 27 Feb 2025 21:33:55 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/video/2025/feb/27/did-i-say-that-donald-trump-denies-calling-zelenskyy-a-dictator-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/eae2dee2a487d8692acec09ee902d3a60f68498d/0_176_4000_2400/master/4000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a0383611c7647d249abc668b49b46bf3">
        <media:credit scheme="urn:ebu">Photograph: UPI/REX/Shutterstock</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/eae2dee2a487d8692acec09ee902d3a60f68498d/0_176_4000_2400/master/4000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=147440513962eabfb99706c7ab3f32e5">
        <media:credit scheme="urn:ebu">Photograph: UPI/REX/Shutterstock</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2025-02-27T21:33:55Z</dc:date>
    </item>
    <item>
      <title>How scientists capture a polar bear – video</title>
      <link>https://www.theguardian.com/environment/video/2025/feb/25/how-scientists-capture-a-polar-bear-video</link>
      <description>&lt;p&gt;Each spring since 2003, Jon Aars, senior scientist at the Norwegian Polar Institute, and his team have conducted an annual &lt;a href="https://www.theguardian.com/environment/2025/feb/25/tracking-polar-bears-svalbard-norway-shifting-ice-melt"&gt;polar bear monitoring&lt;/a&gt; program on Svalbard - collaring, capturing and taking samples from as many bears as they can across several weeks.&lt;br&gt;&lt;br&gt;By studying polar bears they get a better understanding of what is happening in this part of the Arctic environment. The bears roam over large distances and, being apex predators, provide lots of information about what is happening lower in the food chain and across different Arctic species.&lt;br&gt;&lt;br&gt;The Guardian accompanied Aars on an expedition to the southern end of Spitsbergen island, the largest in the Svalbard archipelago.&lt;/p&gt; &lt;a href="https://www.theguardian.com/environment/video/2025/feb/25/how-scientists-capture-a-polar-bear-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/environment/climate-crisis">Climate crisis</category>
      <category domain="https://www.theguardian.com/world/arctic">Arctic</category>
      <category domain="https://www.theguardian.com/environment/poles">Polar regions</category>
      <category domain="https://www.theguardian.com/world/norway">Norway</category>
      <category domain="https://www.theguardian.com/environment/marine-life">Marine life</category>
      <category domain="https://www.theguardian.com/environment/endangered-habitats">Endangered habitats</category>
      <category domain="https://www.theguardian.com/environment/wildlife">Wildlife</category>
      <pubDate>Tue, 25 Feb 2025 08:45:13 GMT</pubDate>
      <guid>https://www.theguardian.com/environment/video/2025/feb/25/how-scientists-capture-a-polar-bear-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/944b478e8278d50b42356459269fa0a567d73a5d/60_0_1800_1080/master/1800.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=05a7031fff3fd7a342d972406b6b9142">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/944b478e8278d50b42356459269fa0a567d73a5d/60_0_1800_1080/master/1800.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=6f0278fe199c26e22a8193e19020e4db">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <dc:creator>Temujin Doran and Katie Lamborn</dc:creator>
      <dc:date>2025-02-25T08:45:13Z</dc:date>
    </item>
    <item>
      <title>Factchecking Donald Trump’s claims about the war in Ukraine – video explainer</title>
      <link>https://www.theguardian.com/us-news/video/2025/feb/20/factchecking-donald-trumps-claims-about-the-war-in-ukraine-video-explainer</link>
      <description>&lt;p&gt;From claiming Ukraine was responsible for the war to incorrect numbers about aid received from the US and Europe, Donald Trump made a number of inaccurate statements while praising the progress made in US-Russia talks in Riyadh, Saudi Arabia. The Guardian has had a look at his claims&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/us-news/2025/feb/19/factchecking-donald-trump-claims-war-ukraine"&gt;Factchecking Donald Trump’s claims about the war in Ukraine&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/us-news/video/2025/feb/20/factchecking-donald-trumps-claims-about-the-war-in-ukraine-video-explainer"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/us-news/donaldtrump">Donald Trump</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/volodymyr-zelenskiy">Volodymyr Zelenskyy</category>
      <category domain="https://www.theguardian.com/world/vladimir-putin">Vladimir Putin</category>
      <pubDate>Thu, 20 Feb 2025 19:56:36 GMT</pubDate>
      <guid>https://www.theguardian.com/us-news/video/2025/feb/20/factchecking-donald-trumps-claims-about-the-war-in-ukraine-video-explainer</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/b95d572035bfa9ad8ac2b982f6c0294bf866c9ac/0_0_6000_3600/master/6000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=b710ed5e6206bfb90f3bf6c41603b469">
        <media:credit scheme="urn:ebu">Photograph: Getty Images</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/b95d572035bfa9ad8ac2b982f6c0294bf866c9ac/0_0_6000_3600/master/6000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=29d5e1f3ce1b0b35c9996e57c29b3127">
        <media:credit scheme="urn:ebu">Photograph: Getty Images</media:credit>
      </media:content>
      <dc:creator>Jakub Krupa, Monika Čvorak and Laure Boulinier</dc:creator>
      <dc:date>2025-02-20T19:56:36Z</dc:date>
    </item>
    <item>
      <title>‘Fix poverty, fix health’: A day in the life of a ‘failing’ NHS</title>
      <link>https://www.theguardian.com/uk-news/video/2025/feb/18/fix-poverty-fix-health-a-day-in-the-life-of-a-failing-nhs</link>
      <description>&lt;p&gt;A GP surgery in one of the most deprived areas in the north-east of England is struggling to provide care for its patients as the health system crumbles around them. In the depths of the winter flu season, the Guardian video producers Maeve Shearlaw and Adam Sich went to Bridges medical practice to shadow the lead GP, Paul Evans, as he worked all hours keep his surgery afloat. Juggling technical challenges, long waiting lists and the profound impact austerity has had on the health of the population, Evans says: 'We are seeing the system fail'&amp;nbsp;&lt;/p&gt; &lt;a href="https://www.theguardian.com/uk-news/video/2025/feb/18/fix-poverty-fix-health-a-day-in-the-life-of-a-failing-nhs"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/uk-news/north-of-england">North of England</category>
      <category domain="https://www.theguardian.com/society/poverty">Poverty</category>
      <category domain="https://www.theguardian.com/society/nhs">NHS</category>
      <category domain="https://www.theguardian.com/society/gps">GPs</category>
      <category domain="https://www.theguardian.com/politics/health">Health policy</category>
      <category domain="https://www.theguardian.com/society/society">Society</category>
      <category domain="https://www.theguardian.com/society/doctors">Doctors</category>
      <category domain="https://www.theguardian.com/society/health">Health</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <pubDate>Tue, 18 Feb 2025 10:36:16 GMT</pubDate>
      <guid>https://www.theguardian.com/uk-news/video/2025/feb/18/fix-poverty-fix-health-a-day-in-the-life-of-a-failing-nhs</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/3b4ee51c0f4975bd52003303abf137d5831bd26d/60_0_1800_1080/master/1800.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=4ab24306250b84e363c22d42e264470c">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/3b4ee51c0f4975bd52003303abf137d5831bd26d/60_0_1800_1080/master/1800.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=8eb237d6a6db4e3b31ad6db075c553af">
        <media:credit scheme="urn:ebu">Photograph: The Guardian</media:credit>
      </media:content>
      <dc:creator>Maeve Shearlaw, Adam Sich, Ken Macfarlane and Temujin Doran</dc:creator>
      <dc:date>2025-02-18T10:36:16Z</dc:date>
    </item>
    <item>
      <title>'Why should we invite them?': Lavrov ridicules European presence at Ukraine peace talks – video</title>
      <link>https://www.theguardian.com/world/video/2025/feb/17/why-should-we-invite-them-lavrov-ridicules-european-presence-at-ukraine-peace-talks-video</link>
      <description>&lt;p&gt;Russia's foreign minister has dismissed the prospect of a place for Europe at talks between the US and Russia to end the fighting in Ukraine. Speaking at a press conference alongside his Serbian counterpart, Sergei Lavrov said: 'If they are going to weasel out some cunning ideas about freezing the conflict, while actually intending – as is their custom, nature and habit – to continue the war, then why should we invite them at all?'&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;p&gt;European leaders have been unnerved by the willingness of Donald Trump, the US president, to engage the Kremlin directly over Ukraine and have been attempting to find a place for themselves in the talks&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/world/live/2025/feb/17/europe-live-european-leaders-paris-ukraine-future-peace-summit-us-russia-latest-updates-news"&gt;Europe live – latest updates&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/world/video/2025/feb/17/why-should-we-invite-them-lavrov-ridicules-european-presence-at-ukraine-peace-talks-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Mon, 17 Feb 2025 14:03:29 GMT</pubDate>
      <guid>https://www.theguardian.com/world/video/2025/feb/17/why-should-we-invite-them-lavrov-ridicules-european-presence-at-ukraine-peace-talks-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/05edfa2e7d062b533e686dd2a7dedc124e875b4c/0_0_3000_1799/master/3000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a0d3e944883756feacd0fa15b6c21b87">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/05edfa2e7d062b533e686dd2a7dedc124e875b4c/0_0_3000_1799/master/3000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=90758f0c0302387ed9a676366f9ee8b6">
        <media:credit scheme="urn:ebu">Photograph: AP</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2025-02-17T14:03:29Z</dc:date>
    </item>
    <item>
      <title>Parents of Alexei Navalny join hundreds of mourners on the anniversary of his death  – video report</title>
      <link>https://www.theguardian.com/world/video/2025/feb/16/parents-of-alexei-navalny-join-hundreds-of-mourners-on-the-anniversary-of-his-death-video-report</link>
      <description>&lt;p&gt;The parents of late Russian opposition leader Alexei Navalny joined hundreds of mourners at their son's grave on Sunday to mark the anniversary of his death. Navalny died aged 47 on 16 February last year while being held in a jail about 40 miles north of the Arctic Circle, where he had been sentenced to 19 years under a ‘special regime’&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/world/2025/feb/16/alexei-navalny-supporters-visit-grave-on-first-anniversary-of-his-death"&gt;&lt;strong&gt;Alexei Navalny supporters visit grave on first anniversary of his death&lt;/strong&gt;&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/world/video/2025/feb/16/parents-of-alexei-navalny-join-hundreds-of-mourners-on-the-anniversary-of-his-death-video-report"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/alexei-navalny">Alexei Navalny</category>
      <category domain="https://www.theguardian.com/world/russia">Russia</category>
      <category domain="https://www.theguardian.com/world/europe-news">Europe</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Sun, 16 Feb 2025 15:34:14 GMT</pubDate>
      <guid>https://www.theguardian.com/world/video/2025/feb/16/parents-of-alexei-navalny-join-hundreds-of-mourners-on-the-anniversary-of-his-death-video-report</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/ec6fa139d6c82b0290508c8ec3a67960e5fc294e/0_317_5940_3563/master/5940.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2a43eb1f745fafdd3ccfe67cdd51155d">
        <media:credit scheme="urn:ebu">Photograph: Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/ec6fa139d6c82b0290508c8ec3a67960e5fc294e/0_317_5940_3563/master/5940.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=6771f2e8af42c5b1e23647e71ee06879">
        <media:credit scheme="urn:ebu">Photograph: Reuters</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2025-02-16T15:34:14Z</dc:date>
    </item>
    <item>
      <title>Netanyahu says Israel working closely with US on Trump’s 'bold vision' for Gaza – video</title>
      <link>https://www.theguardian.com/world/video/2025/feb/16/netanyahu-says-israel-working-closely-with-us-on-trumps-bold-vision-for-gaza-video</link>
      <description>&lt;p&gt;Benjamin Netanyahu has said his government is working closely with the US to implement Donald Trump’s plan for Gaza, which involves US ownership of the coastal strip, the removal of more than 2 million Palestinians and the redevelopment of the occupied territory as a resort. The Israeli prime minister was speaking after a meeting in Jerusalem with the US secretary of state, Marco Rubio, who defended the Trump plan as bold and visionary&lt;/p&gt;&lt;p&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;p&gt;&lt;a href="https://www.theguardian.com/world/2025/feb/16/israel-netanyahu-trump-plan-gaza"&gt;Netanyahu says Israel working closely with US on Trump’s plan for Gaza&lt;/a&gt;&lt;/p&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/world/video/2025/feb/16/netanyahu-says-israel-working-closely-with-us-on-trumps-bold-vision-for-gaza-video"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/gaza">Gaza</category>
      <category domain="https://www.theguardian.com/world/israel-hamas-war">Israel-Gaza war</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/us-news/us-foreign-policy">US foreign policy</category>
      <category domain="https://www.theguardian.com/world/benjamin-netanyahu">Benjamin Netanyahu</category>
      <category domain="https://www.theguardian.com/us-news/marco-rubio">Marco Rubio</category>
      <category domain="https://www.theguardian.com/us-news/trump-administration">Trump administration</category>
      <category domain="https://www.theguardian.com/world/palestinian-territories">Palestinian territories</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Sun, 16 Feb 2025 15:22:46 GMT</pubDate>
      <guid>https://www.theguardian.com/world/video/2025/feb/16/netanyahu-says-israel-working-closely-with-us-on-trumps-bold-vision-for-gaza-video</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/e6adf5598205d4fde0bb6fe20a3d63e7678c3ce5/0_509_7658_4596/master/7658.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=3a78d133fd5f39fdc328e5c386f50559">
        <media:credit scheme="urn:ebu">Photograph: Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/e6adf5598205d4fde0bb6fe20a3d63e7678c3ce5/0_509_7658_4596/master/7658.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=23c3f69d8f863367470b7c02479b816e">
        <media:credit scheme="urn:ebu">Photograph: Reuters</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2025-02-16T15:22:46Z</dc:date>
    </item>
    <item>
      <title>Sign up for the Fashion Statement newsletter: our free fashion email</title>
      <link>https://www.theguardian.com/global/2022/sep/20/sign-up-for-the-fashion-statement-newsletter-our-free-fashion-email</link>
      <description>&lt;p&gt;Style, with substance: what’s really trending this week, a roundup of the best fashion journalism and your wardrobe dilemmas solved, direct to your inbox every Thursday&lt;/p&gt;&lt;p&gt;Style, with substance: what’s really trending this week, a roundup of the best fashion journalism and your wardrobe dilemmas solved, delivered straight to your inbox every Thursday&lt;/p&gt;&lt;p&gt;&lt;strong&gt;&lt;a href="https://www.theguardian.com/email-newsletters"&gt;Explore all our newsletters:&lt;/a&gt;&lt;/strong&gt;&lt;a href="https://www.theguardian.com/email-newsletters"&gt; whether you love film, football, fashion or food, we’ve got something for you&lt;/a&gt;&lt;/p&gt; &lt;a href="https://www.theguardian.com/global/2022/sep/20/sign-up-for-the-fashion-statement-newsletter-our-free-fashion-email"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/info/newsletter-sign-up">Newsletter sign-up</category>
      <category domain="https://www.theguardian.com/fashion/fashion">Fashion</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <pubDate>Tue, 20 Sep 2022 11:06:20 GMT</pubDate>
      <guid>https://www.theguardian.com/global/2022/sep/20/sign-up-for-the-fashion-statement-newsletter-our-free-fashion-email</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/f674446dfd0fef9340436a1bea2e93ec0cdf1905/0_0_760_456/master/760.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=6ef37adb8fd3ec08dc36b56853573318">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/f674446dfd0fef9340436a1bea2e93ec0cdf1905/0_0_760_456/master/760.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=fa590eb398eb19be5f827fe0038d6601">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2022-09-20T11:06:20Z</dc:date>
    </item>
    <item>
      <title>Sign up for the Guardian Documentaries newsletter: our free short film email</title>
      <link>https://www.theguardian.com/info/2016/sep/02/sign-up-for-the-guardian-documentaries-update</link>
      <description>&lt;p&gt;Be the first to see our latest thought-provoking films, bringing you bold and original storytelling from around the world&lt;/p&gt;&lt;p&gt;Discover the stories behind our latest short films, learn more about our international film-makers, and join us for exclusive documentary events. We’ll also share a selection of our favourite films, from our archives and from further afield, for you to enjoy. Sign up below.&lt;/p&gt;&lt;p&gt;Can’t wait for the next newsletter? &lt;a href="https://www.theguardian.com/documentaries"&gt;Start exploring our archive now&lt;/a&gt;.&lt;/p&gt; &lt;a href="https://www.theguardian.com/info/2016/sep/02/sign-up-for-the-guardian-documentaries-update"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/film/documentary">Documentary films</category>
      <category domain="https://www.theguardian.com/info/info">Information</category>
      <category domain="https://www.theguardian.com/tv-and-radio/documentary">Documentary</category>
      <category domain="https://www.theguardian.com/info/newsletter-sign-up">Newsletter sign-up</category>
      <pubDate>Fri, 02 Sep 2016 09:27:20 GMT</pubDate>
      <guid>https://www.theguardian.com/info/2016/sep/02/sign-up-for-the-guardian-documentaries-update</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/4a61421d53a7128125385ef765aa4f02553f18e5/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d117e10d4612bcdaa727e68a10cce8f8">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/4a61421d53a7128125385ef765aa4f02553f18e5/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=545aead3ca362cc3ee6b53fa0fbc2cb2">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <dc:creator>Guardian Staff</dc:creator>
      <dc:date>2016-09-02T09:27:20Z</dc:date>
    </item>
    <item>
      <title>Guardian Traveller newsletter: Sign up for our free holidays email</title>
      <link>https://www.theguardian.com/global/2022/oct/12/sign-up-for-the-guardian-traveller-newsletter-our-free-holidays-email</link>
      <description>&lt;p&gt;From biking adventures to city breaks, get inspiration for your next break – whether in the UK or further afield – with twice-weekly emails from the Guardian’s travel editors. You’ll also receive handpicked offers from Guardian Holidays. &lt;/p&gt;&lt;p&gt;From biking adventures to city breaks, get inspiration for your next break – whether in the UK or further afield – with twice-weekly emails from the Guardian’s travel editors.&lt;/p&gt;&lt;p&gt;You’ll also receive handpicked offers from Guardian Holidays.&lt;/p&gt; &lt;a href="https://www.theguardian.com/global/2022/oct/12/sign-up-for-the-guardian-traveller-newsletter-our-free-holidays-email"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/info/newsletter-sign-up">Newsletter sign-up</category>
      <category domain="https://www.theguardian.com/travel/travel">Travel</category>
      <pubDate>Wed, 12 Oct 2022 14:21:58 GMT</pubDate>
      <guid>https://www.theguardian.com/global/2022/oct/12/sign-up-for-the-guardian-traveller-newsletter-our-free-holidays-email</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/6b8bc65b6cc6ad15c1b60a5f21718aa0449979f4/0_0_760_456/master/760.png?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=d14dbba384ae9b27d6730ec4681b557a">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/6b8bc65b6cc6ad15c1b60a5f21718aa0449979f4/0_0_760_456/master/760.png?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9b62a56cf762fbfa777801a4744dcc95">
        <media:credit scheme="urn:ebu">Illustration: Guardian Design</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2022-10-12T14:21:58Z</dc:date>
    </item>
    <item>
      <title>Sign up for the Feast newsletter: our free Guardian food email</title>
      <link>https://www.theguardian.com/food/2019/jul/09/sign-up-for-the-feast-newsletter-our-free-guardian-food-email</link>
      <description>&lt;p&gt;A weekly email from Yotam Ottolenghi, Meera Sodha, Felicity Cloake and Rachel Roddy, featuring the latest recipes and seasonal eating ideas&lt;/p&gt;&lt;p&gt;Each week we’ll send you an exclusive newsletter from our star food writers. We’ll also send you the latest recipes from Yotam Ottolenghi, Nigel Slater, Meera Sodha and all our star cooks, stand-out food features and seasonal eating inspiration, plus restaurant reviews from Grace Dent and Jay Rayner.&lt;/p&gt;&lt;p&gt;Sign up below to start receiving the best of our culinary journalism in one mouth-watering weekly email.&lt;/p&gt; &lt;a href="https://www.theguardian.com/food/2019/jul/09/sign-up-for-the-feast-newsletter-our-free-guardian-food-email"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/info/info">Information</category>
      <category domain="https://www.theguardian.com/info/newsletter-sign-up">Newsletter sign-up</category>
      <pubDate>Tue, 09 Jul 2019 08:19:21 GMT</pubDate>
      <guid>https://www.theguardian.com/food/2019/jul/09/sign-up-for-the-feast-newsletter-our-free-guardian-food-email</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/0edfd4ddd15717271a97f651cd955949b153b31b/0_0_5000_3000/master/5000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=c2a8fd598e8a452c5ca5e318597805b6">
        <media:credit scheme="urn:ebu">Composite: The Guardian</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/0edfd4ddd15717271a97f651cd955949b153b31b/0_0_5000_3000/master/5000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=deb2e93ce1ac36aa603f3b3b31062fb8">
        <media:credit scheme="urn:ebu">Composite: The Guardian</media:credit>
      </media:content>
      <dc:creator />
      <dc:date>2019-07-09T08:19:21Z</dc:date>
    </item>
    <item>
      <title>Castles in the sky: the fantastical drawings of author Victor Hugo – in pictures</title>
      <link>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/castles-in-the-sky-the-fantastical-drawings-of-author-victor-hugo-in-pictures</link>
      <description>&lt;p&gt;Although better known for his sprawling Romantic novels &lt;em&gt;The Hunchback of Notre-Dame &lt;/em&gt;and &lt;em&gt;Les Misérables&lt;/em&gt;, celebrated French author Victor Hugo spent much of his time drawing. A collection of about 70 of his sketches will soon be on display at the &lt;a href="https://www.royalacademy.org.uk/"&gt;Royal Academy in London&lt;/a&gt;, in an exhibition bringing together caricatures, travel drawings and landscapes. Several of the drawings feature castles and ruins. “Hugo was inspired by ‘burgs’ – castles, fortresses or walled towns – that he saw when travelling along the Rhine, but he often drew fantastical castles that fuse memory and imagination,” says the exhibition’s curator Sarah Lea. “Hugo’s castle drawings range in tone from sinister and sublime to highly romantic and exquisitely detailed.”&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;a href="https://www.royalacademy.org.uk/exhibition/astonishing-things"&gt;Astonishing Things: The Drawings of Victor Hugo is at the Royal Academy of Arts, London W1, 21 March to 29 June&lt;/a&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/artanddesign/gallery/2025/mar/01/castles-in-the-sky-the-fantastical-drawings-of-author-victor-hugo-in-pictures"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/artanddesign/drawing">Drawing</category>
      <category domain="https://www.theguardian.com/books/victor-hugo">Victor Hugo</category>
      <category domain="https://www.theguardian.com/artanddesign/exhibition">Exhibitions</category>
      <category domain="https://www.theguardian.com/artanddesign/art">Art</category>
      <category domain="https://www.theguardian.com/artanddesign/artanddesign">Art and design</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <pubDate>Sat, 01 Mar 2025 17:00:43 GMT</pubDate>
      <guid>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/castles-in-the-sky-the-fantastical-drawings-of-author-victor-hugo-in-pictures</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/20d20929b5c35b7334f260a5e901b6eddff46d6d/832_572_3045_1826/master/3045.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=79e3fe077102c0c6f747b7e0cdb6b098">
        <media:credit scheme="urn:ebu">Photograph: British Museum, London</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/20d20929b5c35b7334f260a5e901b6eddff46d6d/832_572_3045_1826/master/3045.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=1e88b9c41abd5abec8f836fc7df18fdd">
        <media:credit scheme="urn:ebu">Photograph: British Museum, London</media:credit>
      </media:content>
      <dc:creator>Kathryn Bromwich</dc:creator>
      <dc:date>2025-03-01T17:00:43Z</dc:date>
    </item>
    <item>
      <title>Original Observer Photography</title>
      <link>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/original-observer-photography</link>
      <description>&lt;p&gt;From the world of adventure to the worlds of food and theatre: the best original photographs from the Observer commissioned in February 2025&lt;/p&gt; &lt;a href="https://www.theguardian.com/artanddesign/gallery/2025/mar/01/original-observer-photography"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/artanddesign/artanddesign">Art and design</category>
      <category domain="https://www.theguardian.com/artanddesign/photography">Photography</category>
      <category domain="https://www.theguardian.com/books/colum-mccann">Colum McCann</category>
      <category domain="https://www.theguardian.com/food/food">Food</category>
      <category domain="https://www.theguardian.com/stage/theatre">Theatre</category>
      <category domain="https://www.theguardian.com/world/gaza">Gaza</category>
      <category domain="https://www.theguardian.com/world/israel">Israel</category>
      <category domain="https://www.theguardian.com/world/hamas">Hamas</category>
      <category domain="https://www.theguardian.com/artanddesign/martin-parr">Martin Parr</category>
      <category domain="https://www.theguardian.com/food/pasta">Pasta</category>
      <category domain="https://www.theguardian.com/society/rape">Rape and sexual assault</category>
      <category domain="https://www.theguardian.com/culture/pamela-anderson">Pamela Anderson</category>
      <category domain="https://www.theguardian.com/music/cyndi-lauper">Cyndi Lauper</category>
      <category domain="https://www.theguardian.com/fashion/fashion">Fashion</category>
      <category domain="https://www.theguardian.com/culture/tamsin-greig">Tamsin Greig</category>
      <category domain="https://www.theguardian.com/stage/mark-steel">Mark Steel</category>
      <category domain="https://www.theguardian.com/uk/knifecrime">Knife crime</category>
      <category domain="https://www.theguardian.com/world/netherlands">Netherlands</category>
      <category domain="https://www.theguardian.com/sport/rugby-union">Rugby union</category>
      <pubDate>Sat, 01 Mar 2025 11:00:36 GMT</pubDate>
      <guid>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/original-observer-photography</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/ae081799237dc9293d6a750265836d0a1d9d8983/0_58_3084_1850/master/3084.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=75953a7eecf568ffc297b2371becb324">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/ae081799237dc9293d6a750265836d0a1d9d8983/0_58_3084_1850/master/3084.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=623f532c8ac9376cd947d1fd53abc873">
        <media:credit scheme="urn:ebu">Photograph: Sophia Evans/The Observer</media:credit>
      </media:content>
      <dc:creator>Josy Forsdike</dc:creator>
      <dc:date>2025-03-01T11:00:36Z</dc:date>
    </item>
    <item>
      <title>‘The boy jumped at just the right moment’: Pradiptamoy Paul’s best phone picture</title>
      <link>https://www.theguardian.com/lifeandstyle/2025/mar/01/the-boy-jumped-at-just-the-right-moment-pradiptamoy-pauls-best-phone-picture</link>
      <description>&lt;p&gt;The India-based street and documentary photographer captures a group of children in a moment of joy&lt;/p&gt;&lt;p&gt;University student Pradiptamoy Paul currently lives in Siliguri, West Bengal, but he still regularly visits his home town of&amp;nbsp;Mathabhanga, a&amp;nbsp;few hours’ drive away.&amp;nbsp;On the day he took&amp;nbsp;this photograph, back in 2023, he had done some work in the morning and&amp;nbsp;was taking a walk by&amp;nbsp;the Mansai riverside, hoping to capture something&amp;nbsp;special.&lt;/p&gt;&lt;p&gt;“It’s a residential area&amp;nbsp;and there are no industrial sites nearby, so the water here is clean,” Paul says. “In&amp;nbsp;this photo there is so much going on and so many characters. The boy at the front was&amp;nbsp;taking a&amp;nbsp;rest, someone else was&amp;nbsp;splashing in the&amp;nbsp;water, another boy was&amp;nbsp;jumping from the&amp;nbsp;concrete. And the boy jumping from the top corner happened spontaneously, at just the right moment! It’s&amp;nbsp;impossible to say who the actual hero of this photograph is. They’re children immersed in a&amp;nbsp;moment of energy and joy – they’re all heroes.”&lt;/p&gt; &lt;a href="https://www.theguardian.com/lifeandstyle/2025/mar/01/the-boy-jumped-at-just-the-right-moment-pradiptamoy-pauls-best-phone-picture"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/artanddesign/photography">Photography</category>
      <category domain="https://www.theguardian.com/lifeandstyle/lifeandstyle">Life and style</category>
      <category domain="https://www.theguardian.com/artanddesign/art">Art</category>
      <category domain="https://www.theguardian.com/artanddesign/artanddesign">Art and design</category>
      <category domain="https://www.theguardian.com/culture/culture">Culture</category>
      <pubDate>Sat, 01 Mar 2025 10:00:35 GMT</pubDate>
      <guid>https://www.theguardian.com/lifeandstyle/2025/mar/01/the-boy-jumped-at-just-the-right-moment-pradiptamoy-pauls-best-phone-picture</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/8f546985090441bb2dcc0b87890bda8e153f48d7/0_89_3921_2353/master/3921.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=2dce3f091aceb74b0a71c6cf4701fbf1">
        <media:credit scheme="urn:ebu">Photograph: Pradiptamoy Paul</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/8f546985090441bb2dcc0b87890bda8e153f48d7/0_89_3921_2353/master/3921.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=aabf1c34c39fd56c7b2b0705876eb8ae">
        <media:credit scheme="urn:ebu">Photograph: Pradiptamoy Paul</media:credit>
      </media:content>
      <dc:creator>Grace Holliday</dc:creator>
      <dc:date>2025-03-01T10:00:35Z</dc:date>
    </item>
    <item>
      <title>The week around the world in 20 pictures</title>
      <link>https://www.theguardian.com/artanddesign/gallery/2025/feb/28/the-week-around-the-world-in-20-pictures</link>
      <description>&lt;p&gt;Russian bombs hit Ukraine, protests in Greece,  prayers for the Pope and Milan fashion week: the past seven days as captured by the &lt;a href="https://www.instagram.com/_twenty_photos_/"&gt;world’s leading photojournalists&lt;/a&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;&lt;strong&gt;&lt;em&gt;Warning: this gallery contains images that some readers may find distressing&lt;/em&gt;&lt;/strong&gt;&lt;/li&gt;&lt;/ul&gt; &lt;a href="https://www.theguardian.com/artanddesign/gallery/2025/feb/28/the-week-around-the-world-in-20-pictures"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <category domain="https://www.theguardian.com/world/greece">Greece</category>
      <category domain="https://www.theguardian.com/world/palestinian-territories">Palestinian territories</category>
      <category domain="https://www.theguardian.com/world/ukraine">Ukraine</category>
      <category domain="https://www.theguardian.com/world/vatican">Vatican</category>
      <category domain="https://www.theguardian.com/world/germany">Germany</category>
      <category domain="https://www.theguardian.com/world/south-korea">South Korea</category>
      <category domain="https://www.theguardian.com/us-news/us-news">US news</category>
      <category domain="https://www.theguardian.com/fashion/milan-fashion-week">Milan fashion week</category>
      <pubDate>Fri, 28 Feb 2025 19:42:00 GMT</pubDate>
      <guid>https://www.theguardian.com/artanddesign/gallery/2025/feb/28/the-week-around-the-world-in-20-pictures</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/d9266a82ac75d9c7178f3f3f98d62a789b4aea70/0_444_6661_3997/master/6661.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=229753deaf58cc9b24e156e4e0704aad">
        <media:credit scheme="urn:ebu">Photograph: Antonio Calanni/AP</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/d9266a82ac75d9c7178f3f3f98d62a789b4aea70/0_444_6661_3997/master/6661.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=9127fb2eac9e3435a67051b97925204a">
        <media:credit scheme="urn:ebu">Photograph: Antonio Calanni/AP</media:credit>
      </media:content>
      <dc:creator>Jim Powell</dc:creator>
      <dc:date>2025-02-28T19:42:00Z</dc:date>
    </item>
    <item>
      <title>Athens protests and Ramadan preparations: photos of the day – Friday</title>
      <link>https://www.theguardian.com/artanddesign/gallery/2025/feb/28/athens-protests-and-ramadan-preparations-photos-of-the-day-friday</link>
      <description>&lt;p&gt;The Guardian’s picture editors select photographs from around the world&lt;/p&gt; &lt;a href="https://www.theguardian.com/artanddesign/gallery/2025/feb/28/athens-protests-and-ramadan-preparations-photos-of-the-day-friday"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/artanddesign/photography">Photography</category>
      <category domain="https://www.theguardian.com/uk/uk">UK news</category>
      <category domain="https://www.theguardian.com/world/world">World news</category>
      <pubDate>Fri, 28 Feb 2025 13:12:45 GMT</pubDate>
      <guid>https://www.theguardian.com/artanddesign/gallery/2025/feb/28/athens-protests-and-ramadan-preparations-photos-of-the-day-friday</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/a719c857de4a94173cce78d7d31da877e246d99c/54_277_4210_2526/master/4210.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a39f5b3cf6739546db99d47be1e06f24">
        <media:credit scheme="urn:ebu">Photograph: Louisa Gouliamaki/Reuters</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/a719c857de4a94173cce78d7d31da877e246d99c/54_277_4210_2526/master/4210.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=61f26ba6635f30593411e858156cabde">
        <media:credit scheme="urn:ebu">Photograph: Louisa Gouliamaki/Reuters</media:credit>
      </media:content>
      <dc:creator>Paul Bellsham</dc:creator>
      <dc:date>2025-02-28T13:12:45Z</dc:date>
    </item>
    <item>
      <title>From leaping mudskippers to volcanic eruptions: the World Nature Photography awards 2025 – in pictures</title>
      <link>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/the-world-nature-photography-awards-2025-in-pictures</link>
      <description>&lt;p&gt;The &lt;a href="https://www.worldnaturephotographyawards.com/winners-2025"&gt;World Nature Photography awards have announced their winners&lt;/a&gt; for 2025. From white-cheeked terns to a blue-tailed damselfly peeking through a daisy, the photographs are a stark reminder of the beauty and chaos of the natural world. The top award went to Maruša Puhek’s image of two deers running through a Slovenian vineyard&lt;/p&gt; &lt;a href="https://www.theguardian.com/artanddesign/gallery/2025/mar/01/the-world-nature-photography-awards-2025-in-pictures"&gt;Continue reading...&lt;/a&gt;</description>
      <category domain="https://www.theguardian.com/artanddesign/photography">Photography</category>
      <category domain="https://www.theguardian.com/environment/environment">Environment</category>
      <category domain="https://www.theguardian.com/world/animals">Animals</category>
      <category domain="https://www.theguardian.com/environment/marine-life">Marine life</category>
      <category domain="https://www.theguardian.com/environment/wildlife">Wildlife</category>
      <category domain="https://www.theguardian.com/environment/insects">Insects</category>
      <category domain="https://www.theguardian.com/environment/birds">Birds</category>
      <category domain="https://www.theguardian.com/science/animalbehaviour">Animal behaviour</category>
      <pubDate>Fri, 28 Feb 2025 14:00:13 GMT</pubDate>
      <guid>https://www.theguardian.com/artanddesign/gallery/2025/mar/01/the-world-nature-photography-awards-2025-in-pictures</guid>
      <media:content width="140" url="https://i.guim.co.uk/img/media/1c5545816db35bac7dee4b0f7f2bb3d7195f2ee0/0_200_3000_1800/master/3000.jpg?width=140&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=a65946bd0111a2c06fc5ce92179f4a02">
        <media:credit scheme="urn:ebu">Photograph: Georgina Steytler</media:credit>
      </media:content>
      <media:content width="460" url="https://i.guim.co.uk/img/media/1c5545816db35bac7dee4b0f7f2bb3d7195f2ee0/0_200_3000_1800/master/3000.jpg?width=460&amp;quality=85&amp;auto=format&amp;fit=max&amp;s=688a72ac51f402e2758ff2130cee6ccd">
        <media:credit scheme="urn:ebu">Photograph: Georgina Steytler</media:credit>
      </media:content>
      <dc:creator>Guardian Staff</dc:creator>
      <dc:date>2025-02-28T14:00:13Z</dc:date>
    </item>
  </channel>
</rss>`

func TestRssParser(t *testing.T) {
	reader := strings.NewReader(testRss)

	p := RssParser{}
	_, e := p.Parse(reader)

	assert.Nil(t, e)
}
