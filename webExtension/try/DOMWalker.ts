
/*
// cursor - a child position of a parent node, we move cursor down the siblings
// we keep a stack of cursor positions, so a cursor position is restored after going deeper and coming back
dw = DOMWalker()
  .findNodeWithClass("tool__correct") // moves cursor to the node with a class
  .goIn() // go deeper, starts searching through cursor's childs (goOut to go back)
  .findNodeContainingText("80 common phrasal verbs") // moves cursor to the node containing text
  .textContent { text -> jb.set("title", text) } // extract cursor's text and set it to json title property
  .call { jb.startArray("cards") }
  .splitByFunction( // splitByFunctionWithDOMWalker
    dw.findNodeWithClassSplitter("tool__number"),
    { node (cursor's parent), index (child index), length (in sibling count) -> 
      cardDw = DOMWalker(node, index, lenght)
        .findNodeWithClass("tool__number")
        .textContent { text -> jb.set("title", text.trimmed()) }
        .nextSibling() // go to the next sibling
        .textContent { text -> jb.set("definition", text) } 
    }
  )
  .call { jb.endArray() }
*/

class DOMWalker {
  private cursorStack: Array<DOMWalkerCursor> = Array()
  private cursor: DOMWalkerCursor
  private lastFoundResult?: FoundResult = null

  constructor(cursor: DOMWalkerCursor) {
    this.cursor = cursor
  }

  goIn(): DOMWalker {
    let c = this.cursor
    this.cursorStack.push(this.cursor)
    this.cursor = c.inCursor()

    return this
  }

  goInFoundResult(): DOMWalker {
    if (this.lastFoundResult == null) {
      throw new DOMWalkerError(`goIn() failed as lastFoundResult is null`)
    }

    this.cursorStack.push(this.cursor)
    this.cursor = new DOMWalkerCursor(this.lastFoundResult.node, this.lastFoundResult.childIndex)

    return this
  }

  goOut(): DOMWalker {
    this.cursor = this.cursorStack.pop()

    return this
  }

  findNodeWithClass(name: string): DOMWalker {
    let fr = findNodeWithClass(this.cursor.node, this.cursor.childIndex, this.cursor.maxIndex, name)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeWithClass(${name}) failed at ${this.cursor.toString()}`)
    }

    return this
  }

  findNodeContainingText(text: string): DOMWalker {
    let fr = findNodeContainingText(this.cursor.node, this.cursor.childIndex, this.cursor.maxIndex, text)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeContainingText(${text}) failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  textContent(f: (text: string) => void ): DOMWalker {
    f(this.cursor.childNode().textContent)
    return this
  }

  call(f: () => void): DOMWalker {
    f()
    return this
  }
}

class DOMWalkerCursor {
  node: Node
  childIndex: number = -1
  maxIndex: number = -1

  constructor(node: Node);
  constructor(node: Node, childIndex: number);
  constructor(node: Node, childIndex?: number, maxIndex?: number) {
    this.node = node

    if (childIndex != null) {
      this.childIndex = childIndex
    }

    if (maxIndex != null) {
      this.maxIndex = maxIndex
    }
  }

  childNode(): Node | null {
    if (this.childIndex != -1) {
      return this.node.childNodes[this.childIndex]
    } else {
      return null
    }
  }

  inCursor(): DOMWalkerCursor {
    return new DOMWalkerCursor(this.node.childNodes[this.childIndex], 0)
  }

  toString(): String {
    return `node: ${this.node.textContent}\nchildIndex: ${this.childIndex}\nmaxIndex: ${this.maxIndex}`
  }
}

class DOMWalkerError extends Error {

}

//// functions

function splitByFunction(node: Node, startIndex: number, f: (aStartNode: Node, index: number) => FoundResult | null): Array<FoundResult> {
  let result = Array<FoundResult>()
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

function findNodeWithClass(startNode: Node, startIndex: number, endIndex: number, className: String): FoundResult | null {
  if (startIndex == -1 && startNode instanceof Element) {
    var v = startNode.attributes["class"]
    if (v instanceof Attr) {
      if (v.value == className) {
        return new FoundResult(startNode)
      }
    }
  }

  if (startNode instanceof Node) {
    for (let index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeWithClass(element, -1, -1, className)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  return null
}

function findNodeContainingText(startNode: Node, startIndex: number, endIndex: number, text: string): FoundResult | null {
  if (startNode instanceof Node) {
    for (let index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeContainingText(element, -1, -1, text)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  if (startIndex == -1 && startNode instanceof Node) {
    if (startNode.textContent.indexOf(text) != -1) {
      return new FoundResult(startNode)
    }
  }

  return null
}
