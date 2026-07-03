import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Wrench, Loader2 } from 'lucide-react';
import { agentService } from '../services/agentService';
import './AgentPage.css';

export default function AgentPage() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [streamingTokens, setStreamingTokens] = useState('');
  const [activeTools, setActiveTools] = useState([]);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(scrollToBottom, [messages, streamingTokens]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMsg = { role: 'user', content: input };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);
    setStreamingTokens('');
    setActiveTools([]);

    try {
      const ws = agentService.connectStream(
        (data) => {
          if (data.type === 'token') {
            setStreamingTokens((prev) => prev + data.content);
          } else if (data.type === 'tool_start') {
            setActiveTools((prev) => [...prev, { name: data.name, input: data.input, status: 'running' }]);
          } else if (data.type === 'tool_end') {
            setActiveTools((prev) =>
              prev.map((t) =>
                t.name === data.name && t.status === 'running'
                  ? { ...t, status: 'done', output: data.output }
                  : t
              )
            );
          } else if (data.type === 'done') {
            setMessages((prev) => [
              ...prev,
              {
                role: 'assistant',
                content: streamingTokens || '',
                toolCalls: activeTools,
              },
            ]);
            setLoading(false);
            ws.close();
          } else if (data.type === 'error') {
            setMessages((prev) => [...prev, { role: 'assistant', content: `Error: ${data.content}`, error: true }]);
            setLoading(false);
            ws.close();
          }
        },
        () => {
          // WebSocket error fallback — use REST
          fallbackREST(userMsg.content);
        }
      );

      ws.onopen = () => {
        ws.send(JSON.stringify({ message: userMsg.content }));
      };
    } catch {
      fallbackREST(userMsg.content);
    }
  };

  const fallbackREST = async (message) => {
    try {
      const res = await agentService.chat(message);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: res.data.response,
          toolCalls: res.data.tool_calls || [],
        },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'Sorry, I encountered an error. Please try again.', error: true },
      ]);
    }
    setLoading(false);
  };

  // We need to finalize streaming messages with a useEffect
  useEffect(() => {
    if (!loading && streamingTokens) {
      setMessages((prev) => {
        const last = prev[prev.length - 1];
        if (last?.role === 'assistant' && !last.content) {
          return [...prev.slice(0, -1), { ...last, content: streamingTokens }];
        }
        if (last?.role !== 'assistant') {
          return [...prev, { role: 'assistant', content: streamingTokens, toolCalls: activeTools }];
        }
        return prev;
      });
      setStreamingTokens('');
      setActiveTools([]);
    }
  }, [loading]);

  const suggestions = [
    'Show me all patients',
    'Register patient Amit, email amit@test.com, DOB 1992-06-10, address Bangalore',
    'How many patients are registered?',
    'Find patient John',
  ];

  return (
    <div className="agent-page">
      <div className="chat-container">
        <div className="chat-messages">
          {messages.length === 0 && (
            <div className="chat-empty">
              <div className="chat-empty-icon"><Bot size={32} /></div>
              <h3>MedSync AI Assistant</h3>
              <p>I can help you manage patients, billing, and view analytics.</p>
              <div className="suggestions">
                {suggestions.map((s, i) => (
                  <button key={i} className="suggestion-chip" onClick={() => { setInput(s); }}>
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg, i) => (
            <div key={i} className={`message ${msg.role}`}>
              <div className="message-avatar">
                {msg.role === 'user' ? <User size={16} /> : <Bot size={16} />}
              </div>
              <div className="message-body">
                <span className="message-role">{msg.role === 'user' ? 'You' : 'Assistant'}</span>

                {msg.toolCalls?.length > 0 && (
                  <div className="tool-calls">
                    {msg.toolCalls.map((tc, j) => (
                      <div key={j} className="tool-call-card">
                        <div className="tool-call-header">
                          <Wrench size={13} />
                          <span className="tool-call-name">{tc.name}</span>
                          <span className={`tool-status ${tc.status || 'done'}`}>
                            {tc.status === 'running' ? 'Running...' : '✓'}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <div className={`message-content ${msg.error ? 'error' : ''}`}>
                  {msg.content}
                </div>
              </div>
            </div>
          ))}

          {loading && (
            <div className="message assistant">
              <div className="message-avatar"><Bot size={16} /></div>
              <div className="message-body">
                <span className="message-role">Assistant</span>
                {activeTools.length > 0 && (
                  <div className="tool-calls">
                    {activeTools.map((tc, j) => (
                      <div key={j} className="tool-call-card">
                        <div className="tool-call-header">
                          <Wrench size={13} />
                          <span className="tool-call-name">{tc.name}</span>
                          <span className={`tool-status ${tc.status}`}>
                            {tc.status === 'running' ? <Loader2 size={12} className="spin" /> : '✓'}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {streamingTokens ? (
                  <div className="message-content">{streamingTokens}<span className="cursor-blink" /></div>
                ) : (
                  <div className="message-content thinking">Thinking...</div>
                )}
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <form className="chat-input-bar" onSubmit={sendMessage}>
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type a message..."
            disabled={loading}
          />
          <button type="submit" className="send-btn" disabled={loading || !input.trim()}>
            <Send size={16} />
          </button>
        </form>
      </div>
    </div>
  );
}
