import { FoundResult } from "./foundresult"
/*
// DOM parser with a cursor
// cursor - a child position of the current parent node, we move cursor down the siblings when calling find.. methods
// to change the parent we call go.. methods, in this case we keep a stack of cursor positions, so a cursor position is restored after goOut
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
export class DOMWalker {
  private cursorStack: Array<DOMWalkerCursor> = []
  private cursor: DOMWalkerCursor
  private lastFoundResult?: FoundResult = null
  private internalDOMWalker?: DOMWalker = null
  private stateStack: Array<DOMWalkerState> = []

  constructor(cursor: DOMWalkerCursor) {
    this.cursor = cursor
  }

  reset(cursor: DOMWalkerCursor) {
    this.cursorStack = []
    this.cursor = cursor
    this.lastFoundResult = null
    this.internalDOMWalker = null
  }

  pushState(): DOMWalker {
    this.stateStack.push(new DOMWalkerState(this.cursorStack, this.cursor, this.lastFoundResult))

    return this
  }

  popState(): DOMWalker {
    const state = this.stateStack.pop()
    this.cursorStack = state.cursorStack
    this.cursor = state.cursor
    this.lastFoundResult = state.lastFoundResult

    return this
  }

  goIn(): DOMWalker {
    const c = this.cursor
    this.cursorStack.push(this.cursor)
    this.cursor = c.inCursor()

    return this
  }

  goToFoundResult(): DOMWalker {
    if (this.lastFoundResult == null) {
      throw new DOMWalkerError(`goIn() failed as lastFoundResult is null`)
    }

    this.cursorStack.push(this.cursor)
    this.cursor = new DOMWalkerCursor(this.lastFoundResult.node, this.lastFoundResult.childIndex)

    return this
  }

  goInFoundResult(): DOMWalker {
    if (this.lastFoundResult == null) {
      throw new DOMWalkerError(`goIn() failed as lastFoundResult is null`)
    }

    this.cursorStack.push(this.cursor)
    this.cursor = new DOMWalkerCursor(this.lastFoundResult.childNode(), 0)

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

  goToTopNodeWithMatcher(matcher: NodeMatcher): DOMWalker {
    this.findTopNodeWithMatcher(matcher)
    this.goToFoundResult()

    return this
  }

  findTopNodeWithMatcher(matcher: NodeMatcher): DOMWalker {
    const fr = findTopNode(this.cursor.node, this.cursor.range(), matcher)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findTopNodeWithMatcher(${matcher}) failed at ${this.cursor.toString()}`)
    }

    return this
  }

  findDeepestNodeWithMatcher(matcher: NodeMatcher): DOMWalker {
    const fr = findDeepestNode(this.cursor.node, this.cursor.range(), matcher)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findDeepestNodeWithMatcher(${matcher}) failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  splitByMatcherWithDOMWalker(matcher: NodeMatcher, itemCallback: (dw: DOMWalker) => void): DOMWalker {
    const internalDOMWalker = this.requireInternalDOMWalker(this.cursor)
    const foundResults = splitByMatcher(this.cursor.node, this.cursor.range(), matcher)

    for (let index = 0; index < foundResults.length; index++) {
      const fr = foundResults[index]

      if (index == foundResults.length - 1) {
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, -1))
      } else {
        const nextFr = foundResults[index+1]
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, nextFr.childIndexInOriginalNode))
      }
      itemCallback(internalDOMWalker)
    }

    return this
  }

  // splitByFunctionWithDOMWalker(f: (node: Node, range: DOMWalkerRange) => FoundResult | null, itemCallback: (dw: DOMWalker) => void): DOMWalker {
  //   const internalDOMWalker = this.requireInternalDOMWalker(this.cursor)
  //   const foundResults = splitByFunction(this.cursor.node, this.cursor.range(), f)

  //   for (let index = 0; index < foundResults.length; index++) {
  //     const fr = foundResults[index]

  //     if (index == foundResults.length - 1) {
  //       internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, -1))
  //     } else {
  //       const nextFr = foundResults[index+1]
  //       internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, nextFr.childIndexInOriginalNode))
  //     }
  //     itemCallback(internalDOMWalker)
  //   }

  //   return this
  // }

  textContent(f: (text: string) => void ): DOMWalker {
    f(this.cursor.childNode().textContent)
    return this
  }

  childNode(f: (text: Node) => void ): DOMWalker {
    f(this.cursor.childNode())
    return this
  }

  call(f: () => void): DOMWalker {
    f()
    return this
  }

  try(f: (dw: DOMWalker) => void): DOMWalker {
    try {
      f(this)
    } catch (e) {
      console.log(e)
    }

    return this
  }

  whileNotEnd(f: () => void): DOMWalker {
    const p = this.cursor.childIndex
    try {
      while (!this.cursor.atEnd()) {
        f()
        if (p == this.cursor.childIndex) {
          break
        }
      }
    } catch (e) {
      if (!(e instanceof DOMWalkerError)) {
        console.log(e)
      }
    }

    return this
  }

  requireInternalDOMWalker(cursor: DOMWalkerCursor): DOMWalker {
    if (this.internalDOMWalker == null) {
      this.internalDOMWalker = new DOMWalker(cursor)
    }

    return this.internalDOMWalker
  }
}

export class DOMWalkerCursor {
  node: Node
  childIndex = -1
  maxIndex = -1

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

  toString(): string {
    return `node: ${this.node.textContent}\nchildIndex: ${this.childIndex}\nmaxIndex: ${this.maxIndex}`
  }

  range(): DOMWalkerRange {
    return new DOMWalkerRange(this.childIndex, this.maxIndex)
  }

  atEnd(): boolean {
    return this.maxIndex != -1 && this.childIndex >= this.maxIndex || this.childIndex >= this.node.childNodes.length
  }
}

class DOMWalkerState {
  cursorStack: Array<DOMWalkerCursor> = []
  cursor: DOMWalkerCursor
  lastFoundResult?: FoundResult = null

  constructor(cursorStack: Array<DOMWalkerCursor>, cursor: DOMWalkerCursor, lastFoundResult?: FoundResult) {
    this.cursorStack = Array(...cursorStack)
    this.cursor = new DOMWalkerCursor(cursor.node, cursor.childIndex, cursor.maxIndex)
    this.lastFoundResult = lastFoundResult
  }
}

// [startIndex..endIndex)
// -1 for startIndex means we include the node itself
// -1 for endIndex means infinity 
export class DOMWalkerRange {
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

const NodeRange = new DOMWalkerRange(-1, -1)

export class DOMWalkerError extends Error {
  constructor(s: string) {
    super(s)
    Object.setPrototypeOf(this, DOMWalkerError.prototype);
  }
}

//// functions

function splitByMatcher(node: Node, range: DOMWalkerRange, matcher: NodeMatcher): Array<FoundResult> {
  return splitByFunction(node, range, (aStartNode: Node, range: DOMWalkerRange) => {
    return findTopNode(aStartNode, range, matcher)
  })
}

function splitByFunction(node: Node, range: DOMWalkerRange, f: (aNode: Node, aRange: DOMWalkerRange) => FoundResult | null): Array<FoundResult> {
  const result = Array<FoundResult>()
  for (let index = range.start; index < node.childNodes.length && range.isInRange(index);) {
    const fr = f(node, new DOMWalkerRange(index, range.end))
    if (fr != null) {
      result.push(fr)
      index = fr.childIndexInOriginalNode + 1 
    } else {
      ++index
    }
  }

  return result
}

export function findNodeWithMatcher(matcher: NodeMatcher): (aStartNode: Node, range: DOMWalkerRange) => FoundResult | null {
  return (aStartNode: Node, range: DOMWalkerRange) => {
    return findTopNode(aStartNode, range, matcher)
  }
}

// function findNodeWithClass(startNode: Node, range: DOMWalkerRange, className: string): FoundResult | null {
//   if (range.start == -1 && startNode instanceof Element) {
//     const v = startNode.attributes.getNamedItem("class")
//     if (v instanceof Attr) {
//       if (v.value.trim() == className) {
//         return new FoundResult(startNode)
//       }
//     }
//   }

//   if (startNode instanceof Node) {
//     for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
//       const element = startNode.childNodes[index];
//       const fr = findNodeWithClass(element, NodeRange, className)
//       if (fr != null) {
//         if (fr.childIndex == -1) {
//           return new FoundResult(startNode, index, index)
//         }

//         fr.childIndexInOriginalNode = index
//         return fr
//       }
//     };
//   }

//   return null
// }

// function findNodeContainingText(startNode: Node, range: DOMWalkerRange, text: string): FoundResult | null {
//   return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.indexOf(text) != -1 } )
// }

// function findNodeWithNotEmptyText(startNode: Node, range: DOMWalkerRange): FoundResult | null {
//   return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.trim().length > 0 } )
// }

// function findNodeContainingWithTextChecker(startNode: Node, range: DOMWalkerRange, textChecker: (t: string) => boolean): FoundResult | null {
//   if (startNode instanceof Node) {
//     for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
//       const element = startNode.childNodes[index];
//       const fr = findNodeContainingWithTextChecker(element, NodeRange, textChecker)
//       if (fr != null) {
//         if (fr.childIndex == -1) {
//           return new FoundResult(startNode, index, index)
//         }

//         fr.childIndexInOriginalNode = index
//         return fr
//       }
//     };
//   }

//   if (range.start == -1 && startNode instanceof Node) {
//     if (textChecker(startNode.textContent)) {
//       return new FoundResult(startNode)
//     }
//   }

//   return null
// }

export class NodeMatcherBuilder {
  private currentMatcher: NodeMatcher

  constructor(m: NodeMatcher) {
    this.currentMatcher = m
  }

  not(): NodeMatcherBuilder {
    this.currentMatcher = new NotNodeMatcher(this.currentMatcher)
    return this
  }

  and(m: NodeMatcher): NodeMatcherBuilder {
    this.currentMatcher = new AndNodeMatcher(this.currentMatcher, m)
    return this
  }

  or(m: NodeMatcher): NodeMatcherBuilder {
    this.currentMatcher = new OrNodeMatcher(this.currentMatcher, m)
    return this
  }

  build(): NodeMatcher {
    return this.currentMatcher
  }
}

abstract class NodeMatcher {
  abstract match(n: Node): boolean

  asBuilder(): NodeMatcherBuilder {
    return new NodeMatcherBuilder(this)
  }
}

export class NoNodeMatcher extends NodeMatcher {
  match(n: Node): boolean {
    return false
  }
}

export class NotNodeMatcher extends NodeMatcher {
  private matcher: NodeMatcher

  constructor(matcher: NodeMatcher) {
    super()
    this.matcher = matcher
  }

  match(n: Node): boolean {
    return !this.matcher.match(n)
  }
}

export class LambdaNodeMatcher extends NodeMatcher {
  private l: (n:Node) => boolean

  constructor(l: (n:Node) => boolean) {
    super()
    this.l = l
  }

  match(n: Node): boolean {
    return this.l(n)
  }
}

export enum ClassNodeMatcherType {
  Equal,
  StartsWith
}

export class ClassNodeMatcher extends NodeMatcher {
  private className: string
  private type: ClassNodeMatcherType

  constructor(className: string, type: ClassNodeMatcherType = ClassNodeMatcherType.Equal) {
    super()
    this.className = className
    this.type = type
  }

  match(n: Node): boolean {
    if (n instanceof Element) {
      const v = n.attributes.getNamedItem("class")
      if (v instanceof Attr) {
        const trimmed = v.value.trim()
        if (this.type == ClassNodeMatcherType.Equal && trimmed == this.className) {
          return true
        } else if (this.type == ClassNodeMatcherType.StartsWith && trimmed.startsWith(this.className)) {
          return true
        }
      }
    }
    return false
  }
}

export class NodeNameMatcher extends NodeMatcher {
  private name: string

  constructor(name: string) {
    super()
    this.name = name
  }

  match(n: Node): boolean {
    return n.nodeName == this.name
  }
}

export enum TextNodeMatcherType {
  Equal,
  StartsWith,
  EndsWith,
  Contains
}

export class TextNodeMatcher extends NodeMatcher {
  private text: string
  private type: TextNodeMatcherType

  constructor(text: string, type: TextNodeMatcherType = TextNodeMatcherType.Equal) {
    super()
    this.text = text
    this.type = type
  }

  match(n: Node): boolean {
    const trimmed = n.textContent.trim()
    if (this.type == TextNodeMatcherType.Equal && trimmed == this.text) {
      return true
    } else if (this.type == TextNodeMatcherType.StartsWith && trimmed.startsWith(this.text)) {
      return true
    } else if (this.type == TextNodeMatcherType.EndsWith && trimmed.endsWith(this.text)) {
      return true
    } else if (this.type == TextNodeMatcherType.Contains && trimmed.indexOf(this.text) != -1) {
      return true
    }
    return false
  }
}

export class ParentNodeMatcher extends NodeMatcher {
  private parentMatcher: NodeMatcher

  constructor(parentMatcher: NodeMatcher) {
    super()
    this.parentMatcher = parentMatcher
  }

  match(n: Node): boolean {
    let localNode = n
    while (localNode.parentElement != null) {
      if (this.parentMatcher.match(localNode.parentElement)) {
        return true
      }

      localNode = localNode.parentElement
    }

    return false
  }
}

export class AndNodeMatcher extends NodeMatcher {
  private l: NodeMatcher
  private r: NodeMatcher

constructor(l: NodeMatcher, r: NoNodeMatcher) {
    super()
    this.l = l
    this.r = r
  }

  match(n: Node): boolean {
    return this.l.match(n) && this.r.match(n)
  }
}

export class OrNodeMatcher extends NodeMatcher {
  private l: NodeMatcher
  private r: NodeMatcher

  constructor(l: NodeMatcher, r: NoNodeMatcher) {
    super()
    this.l = l
    this.r = r
  }

  match(n: Node): boolean {
    return this.l.match(n) || this.r.match(n)
  }
}

function findTopNode(startNode: Node, range: DOMWalkerRange, matcher: NodeMatcher): FoundResult | null {
  if (range.start == -1 && startNode instanceof Element) {
    if (matcher.match(startNode)) {
      return new FoundResult(startNode)
    }
  }

  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      const fr = findTopNode(element, NodeRange, matcher)
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

function findDeepestNode(startNode: Node, range: DOMWalkerRange, matcher: NodeMatcher): FoundResult | null {
  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      const fr = findDeepestNode(element, NodeRange, matcher)
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
    if (matcher.match(startNode)) {
      return new FoundResult(startNode)
    }
  }

  return null
}

export function cutFirstWord(str: string): string {
  const i = str.indexOf(' ')
  if (i != -1) {
    return str.substring(i+1)
  }

  return str
}
