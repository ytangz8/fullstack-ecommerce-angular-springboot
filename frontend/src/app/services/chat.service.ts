import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ChatMessage } from '../common/chat-message';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {

  private chatUrl = `${environment.bluesBakeryUrl}/chat/stream`;

  constructor() { }

  sendMessage(messages: ChatMessage[]): Observable<string> {
    return new Observable(observer => {
      // Prepare the request body
      const body = messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }));

      // Use fetch API to handle SSE
      fetch(this.chatUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body)
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) {
          throw new Error('No reader available');
        }

        // Read the stream
        const readStream = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              observer.complete();
              return;
            }

            // Decode the chunk
            const chunk = decoder.decode(value, { stream: true });
            
            // Parse SSE format (data: content\n\n)
            const lines = chunk.split('\n');
            for (const line of lines) {
              if (line.startsWith('data:')) {
                // Don't trim - preserve spaces from Ollama response
                const data = line.substring(5);
                // Only skip completely empty data
                if (data.length > 0) {
                  observer.next(data);
                }
              }
            }

            // Continue reading
            readStream();
          }).catch(error => {
            observer.error(error);
          });
        };

        readStream();
      })
      .catch(error => {
        observer.error(error);
      });
    });
  }
}
