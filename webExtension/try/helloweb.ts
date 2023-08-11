
document.body.innerHTML = message;

let n1 = findNodeContainingClass(document.body, -1, -1, "tool__example tool__correct")
if (n1 == null) {
  console.log("tool__example tool__correct not found")
}

let n = findNodeContainingText(n1.node, n1.childIndex + 1, -1, "80 common phrasal verbs")
if (n == null) {
  console.log("not element")
}

// let n2 = findNodeContainingClass(n1.node, n.childIndex + 1, "tool__number")
// if (n2 == null) {
//   console.log("not element")
// }

// let nNode = n.node
// if (nNode instanceof Element) {
//   let chNode = nNode.childNodes[n2.childIndexInOriginalNode]
//   if (chNode instanceof Element) {
//     console.log("found text " + chNode.textContent)
//   }
// }

let frArray = splitByFunction(n1.node, n.childIndex, function(aStartNode: Node, index: number): FindResult | null {
  return findNodeContainingClass(aStartNode, index,  -1, "tool__number")
})

let ranges = Array<[number, number]>()
for(let i=0; i<frArray.length; ++i) {
  if (i == frArray.length-1) {
    ranges.push([frArray[i].childIndexInOriginalNode, -1])
  } else {
    ranges.push([frArray[i].childIndexInOriginalNode, frArray[i+1].childIndexInOriginalNode])
  }
}

let rr = ranges.map(function(range: [number, number], i: number):Array<Node> {
  console.log(n1.node.childNodes[range[0]].textContent)
  return Array()
})

// console.log(ranges)

// for(let i=0; i<frArray.length; ++i) {
//   let n = n1.node.childNodes[frArray[i].childIndexInOriginalNode]
//   if (n instanceof Element) {
//     console.log("found text " + n.textContent)
//   }
// }

function splitByFunction(node: Node, startIndex: number, f: (aStartNode: Node, index: number) => FindResult | null): Array<FindResult> {
  let result = Array<FindResult>()
  for (let index = startIndex; index < node.childNodes.length;) {
    let fr = f(node, index)
    if (fr != null) {
      result.push(fr)
      index = fr.childIndexInOriginalNode + 1 
    } else {
      ++index
    }
  }

  return result
}

function findNodeContainingClass(startNode: Node, startIndex: number, endIndex: number, className: String): FindResult | null {
  if (startIndex == -1 && startNode instanceof Element) {
    var v = startNode.attributes["class"]
    if (v instanceof Attr) {
      if (v.value == className) {
        return new FindResult(startNode)
      }
    }
  }

  if (startNode instanceof Element) {
    for (let index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeContainingClass(element, -1, -1, className)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FindResult(startNode, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  return null
}

function findNodeContainingText(startNode: Node, startIndex: number, endIndex: number, text: string): FindResult | null {
  if (startNode instanceof Element) {
    for (let index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeContainingText(element, -1, -1, text)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FindResult(startNode, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  if (startIndex == -1 && startNode instanceof Element) {
    if (startNode.textContent.indexOf(text) != -1) {
      return new FindResult(startNode)
    }
  }

  return null
}

