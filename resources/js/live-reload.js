const eventSource = new EventSource(`/sse${location.pathname}`);

eventSource.onmessage = (event) => {
    document.body.innerHTML = event.data;
};
