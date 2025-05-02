package main

// TO setup
// sudo ipset create myset-ip-script hash:ip
// sudo iptables -I INPUT -m set --match-set myset-ip-script src -j DROP
// sudo iptables -I FORWARD -m set --match-set myset-ip-script src -j DROP
//
// invoke the tool
//
// sudo ipset save > ./ipset.conf
//
// to run in background
// nohup ./ipset-filler > foo.log 2> foo.err < /dev/null &

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"

	"github.com/lrh3321/ipset-go"
	"github.com/nxadm/tail"
)

type AccessLogRecord struct {
	Referrer   string `json:"http_referer"`
	RemoteAddr string `json:"remote_addr"`
	RequestUrl string `json:"request_uri"`
}

func main() {
	// Create a tail
	t, err := tail.TailFile(
		"/Users/Shared/WordTeacher/nginx/access_log",
		tail.Config{Location: &tail.SeekInfo{Offset: 0, Whence: io.SeekEnd}, Follow: true})
	if err != nil {
		panic(err)
	}

	// Print the text of each received line
	for line := range t.Lines {
		var r AccessLogRecord
		err = json.Unmarshal([]byte(line.Text), &r)
		if err == nil {
			if r.RequestUrl == "/" && r.Referrer == "http://5.35.112.90:9090" {
				fmt.Println(r.RemoteAddr)

				var setname = "myset-ip-script"
				ip, err := net.ResolveIPAddr("ip", r.RemoteAddr)
				if err != nil {
					fmt.Printf("can't parse %s: %s\n", r.RemoteAddr, err.Error())
					continue
				}
				err = ipset.Add(setname, &ipset.Entry{IP: ip.IP})
				if err != nil && err != ipset.ErrEntryExist {
					fmt.Printf("can't add %s: %s\n", r.RemoteAddr, err.Error())
				}
			}
		}
	}
	//hashipType()

	//hashnetType()
}

func hashipType() {
	var setname = "test_hash01"
	err := ipset.Create(setname, ipset.TypeHashIP, ipset.CreateOptions{})
	if err != nil {
		log.Fatal(err)
	}

	defer func() {
		err = ipset.Destroy(setname)
		if err != nil {
			log.Fatal(err)
		}
	}()

	err = ipset.Add(setname, &ipset.Entry{IP: net.IPv4(10, 0, 0, 1).To4()})
	if err != nil {
		log.Fatal(err)
	}

	err = ipset.Add(setname, &ipset.Entry{IP: net.IPv4(10, 0, 0, 5).To4()})
	if err != nil {
		log.Fatal(err)
	}

	set, err := ipset.List(setname)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf(`Name: %s
Type: %s
Header: family inet hashsize %d maxelem %d
Size in memory: %d
References: %d
Number of entries: %d
Members:
`,
		set.SetName,
		set.TypeName,
		set.HashSize,
		set.MaxElements,
		set.SizeInMemory,
		set.References,
		set.NumEntries,
	)

	for _, e := range set.Entries {
		fmt.Println(e.IP.String())
	}

	/*
	   Name: test_hash01
	   Type: hash:ip
	   Header: family inet hashsize 1024 maxelem 65536
	   Size in memory: 296
	   References: 0
	   Number of entries: 2
	   Members:
	   10.0.0.1
	   10.0.0.5
	*/
}

func hashnetType() {
	var setname = "test_hash02"
	err := ipset.Create(setname, ipset.TypeHashNet, ipset.CreateOptions{})
	if err != nil {
		log.Fatal(err)
	}

	defer func() {
		err = ipset.Destroy(setname)
		if err != nil {
			log.Fatal(err)
		}
	}()

	err = ipset.Add(setname, &ipset.Entry{IP: net.IPv4(10, 0, 0, 0).To4(), CIDR: 24})
	if err != nil {
		log.Fatal(err)
	}

	err = ipset.Add(setname, &ipset.Entry{IP: net.IPv4(10, 0, 5, 0).To4(), CIDR: 26})
	if err != nil {
		log.Fatal(err)
	}

	set, err := ipset.List(setname)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf(`Name: %s
Type: %s
Header: family inet hashsize %d maxelem %d
Size in memory: %d
References: %d
Number of entries: %d
Members:
`,
		set.SetName,
		set.TypeName,
		set.HashSize,
		set.MaxElements,
		set.SizeInMemory,
		set.References,
		set.NumEntries,
	)

	for _, e := range set.Entries {
		fmt.Println((&net.IPNet{IP: e.IP, Mask: net.CIDRMask(int(e.CIDR), 32)}).String())
	}

	/*
		Name: test_hash02
		Type: hash:net
		Header: family inet hashsize 1024 maxelem 65536
		Size in memory: 568
		References: 0
		Number of entries: 2
		Members:
		10.0.5.0/26
		10.0.0.0/24
	*/
}
