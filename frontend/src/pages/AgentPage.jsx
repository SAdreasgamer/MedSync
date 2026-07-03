import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Wrench, Loader2 } from 'lucide-react';
import { agentService } from '../services/agentService';
import './AgentPage.css';

export default function AgentPage() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(scrollToBottom, [messages]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMsg = { role: 'user', content: input };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    let hasFinished = false;

    const triggerFallback = () => {
      if (hasFinished) return;
      hasFinished = true;
      setMessages((prev) => {
        const last = prev[prev.length - 1];
        if (
          last &&
          last.role === 'assistant' &&
          !last.content &&
          (!last.toolCalls || last.toolCalls.length === 0)
        ) {
          return prev.slice(0, -1);
        }
        return prev;
      });
      fallbackREST(userMsg.content);
    };

    try {
      // Append a placeholder assistant message that will stream content in real-time
      setMessages((prev) => [...prev, { role: 'assistant', content: '', toolCalls: [] }]);

      const ws = agentService.connectStream(
        userMsg.content,
        (data) => {
          if (data.type === 'token') {
            setMessages((prev) => {
              const last = prev[prev.length - 1];
              if (last && last.role === 'assistant') {
                return [...prev.slice(0, -1), { ...last, content: last.content + data.content }];
              }
              return prev;
            });
          } else if (data.type === 'tool_start') {
            setMessages((prev) => {
              const last = prev[prev.length - 1];
              if (last && last.role === 'assistant') {
                const toolCalls = last.toolCalls || [];
                return [
                  ...prev.slice(0, -1),
                  { ...last, toolCalls: [...toolCalls, { name: data.name, input: data.input, status: 'running' }] },
                ];
              }
              return prev;
            });
          } else if (data.type === 'tool_end') {
            setMessages((prev) => {
              const last = prev[prev.length - 1];
              if (last && last.role === 'assistant') {
                const toolCalls = (last.toolCalls || []).map((t) =>
                  t.name === data.name && t.status === 'running'
                    ? { ...t, status: 'done', output: data.output }
                    : t
                );
                return [...prev.slice(0, -1), { ...last, toolCalls }];
              }
              return prev;
            });
          } else if (data.type === 'done') {
            hasFinished = true;
            setLoading(false);
            ws.close();
          } else if (data.type === 'error') {
            hasFinished = true;
            setMessages((prev) => {
              const last = prev[prev.length - 1];
              if (last && last.role === 'assistant') {
                return [...prev.slice(0, -1), { ...last, content: `Error: ${data.content}`, error: true }];
              }
              return prev;
            });
            setLoading(false);
            ws.close();
          }
        },
        () => {
          triggerFallback();
        },
        () => {
          triggerFallback();
        }
      );
    } catch {
      triggerFallback();
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
                            {tc.status === 'running' ? (
                              <Loader2 size={12} className="spin" style={{ color: 'var(--color-warning)' }} />
                            ) : (
                              '✓'
                            )}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <div className={`message-content ${msg.error ? 'error' : ''}`}>
                  {msg.content ? (
                    <>
                      {msg.content}
                      {loading && i === messages.length - 1 && <span className="cursor-blink" />}
                    </>
                  ) : (
                    loading && i === messages.length - 1 && (
                      <span className="thinking-text">Thinking...</span>
                    )
                  )}
                </div>
              </div>
            </div>
          ))}

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
