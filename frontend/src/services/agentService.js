import api from './api';

const AGENT_REST = '/agent';
const AGENT_WS_BASE = import.meta.env.VITE_AGENT_WS_URL || 'ws://localhost:4003';

export const agentService = {
  chat: (message) => api.post(`${AGENT_REST}/chat`, { message }),

  connectStream: (onMessage, onError) => {
    const ws = new WebSocket(`${AGENT_WS_BASE}/agent/stream`);

    ws.onopen = () => {
      console.log('Agent WebSocket connected');
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (e) {
        console.error('Failed to parse WS message:', e);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      if (onError) onError(error);
    };

    ws.onclose = () => {
      console.log('Agent WebSocket closed');
    };

    return ws;
  },
};
