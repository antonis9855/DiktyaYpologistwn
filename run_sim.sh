#!/bin/bash
cd "$(dirname "$0")"

rm -rf shared_directory_* sim_*.log

java AuctionServer > sim_server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"
sleep 1

java Peer alice   pass1 sim > sim_alice.log   2>&1 &
java Peer bob     pass2 sim > sim_bob.log     2>&1 &
java Peer carol   pass3 sim > sim_carol.log   2>&1 &
java Peer dave    pass4 sim > sim_dave.log    2>&1 &
java Peer eve     pass5 sim > sim_eve.log     2>&1 &
echo "5 peers started."

echo "Running for 90 seconds..."
sleep 90

echo ""
echo "=== SERVER LOG ==="
cat sim_server.log

echo ""
echo "=== ALICE ==="
cat sim_alice.log

echo ""
echo "=== BOB ==="
cat sim_bob.log

echo ""
echo "=== CAROL ==="
cat sim_carol.log

echo ""
echo "=== DAVE ==="
cat sim_dave.log

echo ""
echo "=== EVE ==="
cat sim_eve.log

echo ""
echo "=== SHARED DIRECTORIES ==="
for d in shared_directory_*; do
    echo "$d: $(ls $d 2>/dev/null | tr '\n' ' ')"
done

kill $SERVER_PID 2>/dev/null
pkill -f "java Peer" 2>/dev/null
echo "Done."
