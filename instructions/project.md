# Purpose of this project

This is intended to be a plugin for slack for my use to help me manage slack better.  I receive hundreds of
notifications and direct message's a day, many of which I don't care much about.  This project should watch my usage of 
slack to learn from it and understand what is interesting to me based on my behavior of responding to notifications,
reading the message and time spent looking at them.  Likewise many slack channels have new messages that I rarely read,
this tool should also observe my interactions with channels and learn to understand which channels interest me even
if I'm not explicitly tagged in them.  Once the project understands my behavior it should help me by highlight the
things that are interesting to me so that I see them first and can focus on them.  It should do this in the slack
app itself for me.

## Technical details

### Technology choices
This project is written in Java 25 as a Windows application to interact with the locally installed Slack app.  It should
use any local PC resources it can to help learn my behavior, including using a local GPU or NPU when present.  It should have an
AI driven learning model that runs locally and is trained by my behavior.

### User model
To help train its model the implementation should be able to watch my behavior and also allow me to have hints for it,
such as what I like or do not like to provide a feedback loop.

### Testing and validation
The project should have a set of unit tests that cover all functionality of the application to help ensure changes
don't break it.  It should also use PMD and Codestyle to enforce code style rules for all code.  It should use junit as
the test harness.

When it comes to testing the learning model, the app should have training data and split it into training data and test
data following best practices for training an AI model.  There should be data not used as part of training that is used
to validate and evaluate the effectiveness of the model.