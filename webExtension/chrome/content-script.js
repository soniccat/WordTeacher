// alert('Hello World!');
// document.body.style.backgroundColor = "orange"

e = document.getElementsByClassName("SetPageTermChunk")
console.log(e)

words = new Array()

for (let i = 0; i < e.length; i++) {
  e[i].before("Injected text")
  
  termBlocks = e[i].getElementsByClassName("SetPageTerm")
  for (let i = 0; i < termBlocks.length; i++) {
    termElement = termBlocks[i].getElementsByClassName("SetPageTerm-wordText")
    defElement = termBlocks[i].getElementsByClassName("SetPageTerm-definitionText")
    // console.log(termElement)
    term = termElement[0].innerText
    def = defElement[0].innerText
    // console.log(term + " - " + def)
    words.push({"term":term, "def":def})
  }
}

console.log(words)
