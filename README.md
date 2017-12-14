## Description

This generates a dependency graph of your [Pivotal Tracker](https://www.pivotaltracker.com/) stories for a given epic using [Graphviz](http://www.graphviz.org/).
Dependencies are constructed using the [blockers API](https://www.pivotaltracker.com/help/api/rest/v5#Blockers).

## Setup

Graphviz and a PivotalTracker account are required. To install Graphviz on OSX: `brew install graphviz`.

## Usage

`PT_API_TOKEN="X" PT_PROJECT_ID="Y" lein run Z`

* X is your [PivotTracker API Token](https://www.pivotaltracker.com/help/articles/api_token/)
* Y is your project id which is the number in a project's url
* Z is the epic id to visualize. Can be found in the [epic card](https://www.pivotaltracker.com/help/articles/linking_related_stories_and_epics/#using-storyepic-links)

## Credits

@tomjkidd for the initial spike and heavy lifting. @reifyhealth for the time to do it

## License

Copyright © Reify Health, Inc. See LICENSE for more.
