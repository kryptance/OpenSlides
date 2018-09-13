import { BaseModel } from '../base.model';
import { AssignmentUser } from './assignment-user';
import { Poll } from './poll';

/**
 * Representation of an assignment.
 * @ignore
 */
export class Assignment extends BaseModel {
    public id: number;
    public title: string;
    public description: string;
    public open_posts: number;
    public phase: number;
    public assignment_related_users: AssignmentUser[];
    public poll_description_default: number;
    public polls: Poll[];
    public agenda_item_id: number;
    public tags_id: number[];

    public constructor(input?: any) {
        super('assignments/assignment', input);
    }

    public get candidateIds(): number[] {
        return this.assignment_related_users
            .sort((a: AssignmentUser, b: AssignmentUser) => {
                return a.weight - b.weight;
            })
            .map((candidate: AssignmentUser) => candidate.user_id);
    }

    public deserialize(input: any): void {
        Object.assign(this, input);

        this.assignment_related_users = [];
        if (input.assignment_related_users instanceof Array) {
            input.assignment_related_users.forEach(assignmentUserData => {
                this.assignment_related_users.push(new AssignmentUser(assignmentUserData));
            });
        }

        this.polls = [];
        if (input.polls instanceof Array) {
            input.polls.forEach(pollData => {
                this.polls.push(new Poll(pollData));
            });
        }
    }

    public toString(): string {
        return this.title;
    }
}

BaseModel.registerCollectionElement('assignments/assignment', Assignment);
