import { config } from "./config"
import { ParserResult } from "./parser_result";

const url = new URL(document.URL)
const site = config.get(url.hostname)

console.log("script : " + url + " " + site)
console.log("script : " + url.hostname + " " + config)

if (site != null) {
  for (const entry of Array.from(site.entries())) {
    const key = entry[0];
    const value = entry[1];

    if (url.pathname.startsWith(key)) {
      const result = value.call(this, document) as ParserResult
      const jsonString = JSON.stringify(result.cardSetBuilder.json(), null, 4)
      console.log(jsonString)

      const copyCardSetElement = document.createElement("p");
      copyCardSetElement.textContent = "Copy CardSet"
      copyCardSetElement.addEventListener('click', () => {
        navigator.clipboard.writeText(jsonString).then(() => {
          copyCardSetElement.textContent = "success"
        }, () => {
          copyCardSetElement.textContent = "failed"
        });
      })

      result.titleNode.appendChild(copyCardSetElement)
      break
    }
  }
}
