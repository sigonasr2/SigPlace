if [ -z "$1" ]
  then
    echo ""
    echo "  Usage: ./sig <command> {args}"
    echo ""
    echo "  Command List:"
    ls -1 ./scripts | sed -e 's/\.sh$//' | sed -e 's/^/    /'
    echo ""
    exit
fi

./scripts/$1.sh "${*:2}"