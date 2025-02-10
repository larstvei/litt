const eventSource = new EventSource(`/sse${location.pathname}`);

eventSource.onmessage = (event) => {
    document.body.outerHTML = event.data;
};
