import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../common/chat-message';

@Component({
  selector: 'app-chat-widget',
  standalone: false,
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.css']
})
export class ChatWidgetComponent implements OnInit, AfterViewChecked {

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  isOpen: boolean = false;
  messages: ChatMessage[] = [];
  userInput: string = '';
  isLoading: boolean = false;
  private shouldScrollToBottom: boolean = false;

  constructor(private chatService: ChatService) { }

  ngOnInit(): void {
    // Add a welcome message
    this.messages.push({
      role: 'assistant',
      content: 'Hello! I\'m your AI assistant. How can I help you today?',
      timestamp: new Date()
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.shouldScrollToBottom = true;
    }
  }

  sendMessage(): void {
    if (!this.userInput.trim() || this.isLoading) {
      return;
    }

    // Add user message
    const userMessage: ChatMessage = {
      role: 'user',
      content: this.userInput,
      timestamp: new Date()
    };
    this.messages.push(userMessage);
    this.shouldScrollToBottom = true;

    // Clear input
    const messageToSend = this.userInput;
    this.userInput = '';
    this.isLoading = true;

    // Create AI message placeholder
    const aiMessage: ChatMessage = {
      role: 'assistant',
      content: '',
      timestamp: new Date()
    };
    this.messages.push(aiMessage);

    // Send to backend and stream response
    this.chatService.sendMessage(this.messages.filter(m => m.content !== '')).subscribe({
      next: (chunk: string) => {
        // Append chunk to the last message (AI response)
        const lastMessage = this.messages[this.messages.length - 1];
        lastMessage.content += chunk;
        this.shouldScrollToBottom = true;
      },
      error: (error) => {
        console.error('Error:', error);
        const lastMessage = this.messages[this.messages.length - 1];
        lastMessage.content = 'Sorry, I encountered an error. Please try again.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
        this.shouldScrollToBottom = true;
      }
    });
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = 
          this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Scroll error:', err);
    }
  }
}
