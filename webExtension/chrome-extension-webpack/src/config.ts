import { vocabularyListParser } from "./parsers/vocabulary_com/lists/vocabulary_lists_parser";

// eslint-disable-next-line @typescript-eslint/ban-types
export const config: Map<string, Map<string,Function>> = new Map([
  ['www.vocabulary.com', new Map([
    ['/lists', (entity: Document) => {
      return vocabularyListParser(entity)
    }],
    ])
  ],
]);
