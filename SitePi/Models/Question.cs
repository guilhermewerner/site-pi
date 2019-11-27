using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using SitePi.Models;

namespace SitePi.Models
{
    public class Question
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Text { get; set; }

        public int QuizId { get; set; }
        public Quiz Quiz { get; set; }

        public List<Choice> Choices { get; set; }
    }
}
