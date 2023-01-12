Run as:

 docker run \
    --network host \
    mlp-dummy-action

or

 docker run \
    --env MLP_GRPC_HOST=mlp-gate:10601 --env MLP_SERVICE_TOKEN=8974598357943 \
    mlp-dummy-action
