#!/bin/sh

export GIT_INDEX_FILE=tmp.index
trap 'rm tmp.index' EXIT
git read-tree master &&
git ls-files --stage |
sed 's/^100644 \(.*\.\(sh\|exe\|dll\)\)$/100755 \1/g' |
git update-index --index-info &&
printf 'Mark .exe, .dll and .sh files as executable\n\nSigned-off-by: %s\n' \
	 "$(git config user.name) <$(git config user.email)>"|
git update-ref refs/heads/master $(git commit-tree $(git write-tree) -p master)
