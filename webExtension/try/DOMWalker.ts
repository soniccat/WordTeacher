
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

// [startIndex..endIndex)
// -1 for startIndex means we include the node itself
// -1 for endIndex means infinity 
class DOMWalkerRange {
  start: number
  end: number

  constructor(start: number, end: number) {
    this.start = start
    this.end = end
  }

  isInRange(v: number): boolean {
    return v >= this.start && (this.end == -1 || v < this.end)
  }

  rangeWithStart(start: number): DOMWalkerRange {
    return new DOMWalkerRange(start, this.end)
  }

  rangeWithEnd(end: number): DOMWalkerRange {
    return new DOMWalkerRange(this.start, end)
  }
}

let NodeRange = new DOMWalkerRange(-1, -1)

class DOMWalker {
  private cursorStack: Array<DOMWalkerCursor> = Array()
  private cursor: DOMWalkerCursor
  private lastFoundResult?: FoundResult = null
  private internalDOMWalker?: DOMWalker = null

  constructor(cursor: DOMWalkerCursor) {
    this.cursor = cursor
  }

  reset(cursor: DOMWalkerCursor) {
    this.cursorStack = Array()
    this.cursor = cursor
    this.lastFoundResult = null
    this.internalDOMWalker = null
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

  nextSibling(): DOMWalker {
    if (this.cursor.childIndex + 1 == this.cursor.node.childNodes.length) {
      throw new DOMWalkerError("nextSibling is called at the last node")
    }
    this.cursor.childIndex += 1

    return this
  }

  findNodeWithClass(name: string): DOMWalker {
    let fr = findNodeWithClass(this.cursor.node, this.cursor.range(), name)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeWithClass(${name}) failed at ${this.cursor.toString()}`)
    }

    return this
  }

  findNodeContainingText(text: string): DOMWalker {
    let fr = findNodeContainingText(this.cursor.node, this.cursor.range(), text)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeContainingText(${text}) failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  findNodeWithNotEmptyText(): DOMWalker {
    let fr = findNodeWithNotEmptyText(this.cursor.node, this.cursor.range())
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeWithNotEmptyText() failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  splitByFunctionWithDOMWalker(f: (node: Node, range: DOMWalkerRange) => FoundResult | null, itemCallback: (dw: DOMWalker) => void) {
    let internalDOMWalker = this.requireInternalDOMWalker(this.cursor)
    let foundResults = splitByFunction(this.cursor.node, this.cursor.range(), f)

    for (let index = 0; index < foundResults.length; index++) {
      let fr = foundResults[index]

      if (index == foundResults.length - 1) {
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, -1))
      } else {
        let nextFr = foundResults[index+1]
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, nextFr.childIndexInOriginalNode))
      }
      itemCallback(internalDOMWalker)
    }
  }

  textContent(f: (text: string) => void ): DOMWalker {
    f(this.cursor.childNode().textContent)
    return this
  }

  call(f: () => void): DOMWalker {
    f()
    return this
  }

  whileNotEnd(f: () => void) {
    var p = this.cursor.childIndex
    try {
      while (!this.cursor.atEnd()) {
        f()
        if (p == this.cursor.childIndex) {
          break
        }
      }
    } catch (e) {
    }
  }

  requireInternalDOMWalker(cursor: DOMWalkerCursor): DOMWalker {
    if (this.internalDOMWalker == null) {
      this.internalDOMWalker = new DOMWalker(cursor)
    }

    return this.internalDOMWalker
  }
}

class DOMWalkerCursor {
  node: Node
  childIndex: number = -1
  maxIndex: number = -1

  constructor(node: Node);
  constructor(node: Node, childIndex: number);
  constructor(node: Node, childIndex?: number, maxIndex?: number);
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

  range(): DOMWalkerRange {
    return new DOMWalkerRange(this.childIndex, this.maxIndex)
  }

  atEnd(): boolean {
    return this.childIndex >= this.maxIndex || this.childIndex >= this.node.childNodes.length
  }
}

class DOMWalkerError extends Error {
}

//// functions

function splitByFunction(node: Node, range: DOMWalkerRange, f: (aNode: Node, aRange: DOMWalkerRange) => FoundResult | null): Array<FoundResult> {
  let result = Array<FoundResult>()
  for (let index = range.start; index < node.childNodes.length && range.isInRange(index);) {
    let fr = f(node, new DOMWalkerRange(index, range.end))
    if (fr != null) {
      result.push(fr)
      index = fr.childIndexInOriginalNode + 1 
    } else {
      ++index
    }
  }

  return result
}

function findNodeWithClassSplitter(className: string): (aStartNode: Node, range: DOMWalkerRange) => FoundResult | null {
  return (aStartNode: Node, range: DOMWalkerRange) => {
    return findNodeWithClass(aStartNode, range, className)
  }
}

function findNodeWithClass(startNode: Node, range: DOMWalkerRange, className: string): FoundResult | null {
  if (range.start == -1 && startNode instanceof Element) {
    var v = startNode.attributes["class"]
    if (v instanceof Attr) {
      if (v.value == className) {
        return new FoundResult(startNode)
      }
    }
  }

  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeWithClass(element, NodeRange, className)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  return null
}

function findNodeContainingText(startNode: Node, range: DOMWalkerRange, text: string): FoundResult | null {
  return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.indexOf(text) != -1 } )
}

function findNodeWithNotEmptyText(startNode: Node, range: DOMWalkerRange): FoundResult | null {
  return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.trim().length > 0 } )
}

function findNodeContainingWithTextChecker(startNode: Node, range: DOMWalkerRange, textChecker: (t: string) => boolean): FoundResult | null {
  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      var fr = findNodeContainingWithTextChecker(element, NodeRange, textChecker)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  if (range.start == -1 && startNode instanceof Node) {
    if (textChecker(startNode.textContent)) {
      return new FoundResult(startNode)
    }
  }

  return null
}

function cutFirstWord(str: string): string {
  let i = str.indexOf(' ')
  if (i != -1) {
    return str.substring(i+1)
  }

  return str
}